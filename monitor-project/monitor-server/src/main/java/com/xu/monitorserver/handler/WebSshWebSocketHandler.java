package com.xu.monitorserver.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xu.monitorserver.service.sshservice.ISshService;
import com.xu.monitorserver.service.sshservice.SshErrorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;

/**
 * WebSSH 的 WebSocket 消息处理器。
 *
 * <h2>前后端消息协议约定（文本消息）</h2>
 * <ol>
 *   <li>
 *     建立 WebSocket 连接后，前端需要先发送一条 <b>connect</b> 消息，用于告诉服务端要连接的 SSH 目标信息。
 *     <pre>
 *     {
 *       "operate": "connect",
 *       "host": "1.2.3.4",
 *       "port": 22,
 *       "username": "root",
 *       "authType": "password" | "publicKey",
 *       "password": "...",          // authType=password 时必填
 *       "privateKey": "...pem...",  // authType=publicKey 时必填
 *       "passphrase": "..."         // 可选
 *     }
 *     </pre>
 *   </li>
 *   <li>
 *     连接成功后，服务端会发送：<code>{"type":"ready"}</code>，前端据此切换终端为可输入状态。
 *   </li>
 *   <li>
 *     之后前端每次按键/粘贴都会发送 <b>command</b> 消息，服务端将内容写入 SSH Shell 通道。
 *     <pre>
 *     { "operate": "command", "command": "ls\n" }
 *     </pre>
 *   </li>
 *   <li>
 *     错误消息统一为：<code>{"type":"error","code":"...","message":"..."}</code>
 *     其中 code 为稳定的错误码（用于前端精确提示/埋点），message 为面向用户的提示文案。
 *   </li>
 * </ol>
 *
 * <p>说明：该类只负责“协议解析与路由”，真正的 SSH 连接建立、数据收发由 {@link ISshService} 完成。</p>
 */
@Component
public class WebSshWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSshWebSocketHandler.class);

    /**
     * 认证方式：用户名密码
     */
    private static final String AUTH_TYPE_PASSWORD = "password";

    /**
     * 认证方式：用户名 + 私钥（也兼容旧字段 key）
     */
    private static final String AUTH_TYPE_PUBLIC_KEY = "publicKey";

    private final ISshService sshService;

    public WebSshWebSocketHandler(ISshService sshService) {
        this.sshService = sshService;
    }

    /**
     * JSON 解析器：用于把前端发来的 JSON 文本解析为 Map。
     *
     * <p>这里没有定义 DTO，是为了保持协议灵活与减少改动成本；如果后续协议稳定，建议引入 DTO + 校验注解。</p>
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立后触发。
     *
     * <p>注意：此时前端只是“连上 WebSocket”，并不代表 SSH 已连接成功。
     * SSH 目标信息需要前端发送 operate=connect 后才能建立。</p>
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        logger.info("WebSSH WebSocket 连接建立: {}", session.getId());
    }

    /**
     * 收到前端消息时触发。
     *
     * <p>只处理文本消息（TextMessage）。非文本消息直接忽略。</p>
     */
    @Override
    public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message) {
        // 只处理文本消息：二进制消息目前不在协议里
        if (!(message instanceof TextMessage textMessage)) {
            return;
        }

        // 前端 JSON 文本
        String payload = textMessage.getPayload();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);

            // operate 决定消息类型：connect / command
            String operate = (String) data.get("operate");

            // 1) 建立 SSH 连接
            if ("connect".equals(operate)) {
                handleConnect(session, data);
                return;
            }

            // 2) 向远端发送终端输入（按键、粘贴、回车等）
            if ("command".equals(operate)) {
                String command = (String) data.get("command");
                if (command != null) {
                    sshService.recvClientCommand(session, command);
                }
                return;
            }

            // 未知操作类型：返回协议错误
            sendError(session, SshErrorRegistry.WS_BAD_REQUEST, "不支持的操作类型: " + operate);
        } catch (Exception e) {
            // 任何 JSON 解析/类型转换异常，都视为协议错误
            logger.error("消息解析失败", e);
            sendError(session, SshErrorRegistry.WS_BAD_REQUEST, SshErrorRegistry.userMessageOf(SshErrorRegistry.WS_BAD_REQUEST));
        }
    }

    /**
     * 处理 connect 消息（建立 SSH 连接）。
     *
     * <p>这里做最基础的必填校验，避免把明显的空参数传到 SSH 层。</p>
     */
    private void handleConnect(@NonNull WebSocketSession session, @NonNull Map<String, Object> data) {
        // 目标主机信息
        String host = (String) data.get("host");

        // 端口允许传 number/string，最终统一按 int 解析
        int port = data.get("port") != null ? Integer.parseInt(String.valueOf(data.get("port"))) : 22;

        String username = (String) data.get("username");

        // 认证方式：password / publicKey（默认 password 以兼容旧前端）
        String authType = data.get("authType") != null ? String.valueOf(data.get("authType")) : AUTH_TYPE_PASSWORD;

        // 基础参数校验
        if (host == null || host.isBlank()) {
            sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：请填写主机 IP");
            return;
        }
        if (username == null || username.isBlank()) {
            sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：请填写用户名");
            return;
        }

        // 分流 1：用户名 + 密码
        if (AUTH_TYPE_PASSWORD.equalsIgnoreCase(authType)) {
            String password = (String) data.get("password");
            if (password == null || password.isBlank()) {
                sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：请填写密码");
                return;
            }
            sshService.initConnectionByPassword(session, host, port, username, password);
            return;
        }

        // 分流 2：用户名 + 私钥（可选口令）
        if (AUTH_TYPE_PUBLIC_KEY.equalsIgnoreCase(authType) || "key".equalsIgnoreCase(authType)) {
            String privateKey = (String) data.get("privateKey");
            String passphrase = (String) data.get("passphrase");

            if (privateKey == null || privateKey.isBlank()) {
                sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：请粘贴私钥内容");
                return;
            }
            sshService.initConnectionByPrivateKey(session, host, port, username, privateKey, passphrase);
            return;
        }

        // 其他 authType：统一返回参数错误
        sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：不支持的认证方式 " + authType);
    }

    /**
     * 向前端发送错误消息。
     *
     * <p>结构固定：type=error + code + message。</p>
     * <p>这里手动拼 JSON，是为了不引入额外 DTO；escapeJson 仅做最基本的转义，避免破坏 JSON 格式。</p>
     */
    private void sendError(@Nullable WebSocketSession session, @Nullable String code, @Nullable String message) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage("{\"type\":\"error\",\"code\":\"" + escapeJson(code) + "\",\"message\":\"" + escapeJson(message) + "\"}"));
            }
        } catch (Exception ignored) {
            // 避免二次异常导致 handler 崩溃；此处静默即可
        }
    }

    /**
     * 最小化 JSON 字符串转义：
     * <ul>
     *     <li>反斜杠、双引号转义</li>
     *     <li>过滤 \r，\n 转为字面量 \n，避免破坏 JSON 结构</li>
     * </ul>
     */
    private String escapeJson(@Nullable String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    /**
     * WebSocket 底层传输异常（网络抖动、连接重置等）时触发。
     *
     * <p>策略：关闭对应 SSH 资源，避免“WebSocket 已断开但 SSH 仍占用”的泄漏。</p>
     */
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        logger.error("WebSocket 传输异常", exception);
        sshService.close(session);
    }

    /**
     * WebSocket 正常关闭时触发。
     *
     * <p>同样需要释放 SSH 连接资源。</p>
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus closeStatus) {
        logger.info("WebSSH 连接断开: {}", session.getId());
        sshService.close(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
