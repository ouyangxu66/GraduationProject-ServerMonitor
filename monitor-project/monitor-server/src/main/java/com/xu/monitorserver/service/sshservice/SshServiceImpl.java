package com.xu.monitorserver.service.sshservice;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebSSH 核心服务实现：负责使用 JSch 建立 SSH Shell 会话，并把 WebSocket 的输入输出桥接到远端。
 *
 * <h2>整体数据流</h2>
 * <ul>
 *   <li>前端通过 WebSocket 发送 connect 消息 → 调用 initConnection... 建立 SSH</li>
 *   <li>前端发送 command 消息 → {@link #recvClientCommand(WebSocketSession, String)} 写入 SSH 的 stdin</li>
 *   <li>服务端线程读取 SSH 的 stdout/stderr（shell 通道）→ 推送给前端 WebSocket 文本消息</li>
 * </ul>
 *
 * <p><b>并发/资源说明：</b>每个 WebSocketSession 对应一个 SSH Session + ChannelShell，存放在 {@link #sshMap}。
 * WebSocket 断开/异常时必须及时 {@link #close(WebSocketSession)}，避免连接泄漏。</p>
 */
@Service
public class SshServiceImpl implements ISshService {

    private static final Logger logger = LoggerFactory.getLogger(SshServiceImpl.class);

    /**
     * 保存所有已建立的 SSH 连接。
     *
     * <p>key：WebSocket SessionId</p>
     * <p>value：封装的 SSH JSch Session + Shell 通道</p>
     */
    private static final Map<String, SshConnection> sshMap = new ConcurrentHashMap<>();

    /**
     * 线程池：用于异步读取远端 Shell 回显。
     *
     * <p>这里使用 cachedThreadPool：连接数增多时会动态扩容线程。
     * 若未来连接规模大，建议使用自定义线程池（限制最大线程数）并增加背压策略。</p>
     */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 兼容旧接口：如果外部仍调用 initConnection，则默认走“用户名密码”方式。
     */
    @Override
    public void initConnection(WebSocketSession session, String ip, int port, String username, String password) {
        initConnectionByPassword(session, ip, port, username, password);
    }

    /**
     * 用户名 + 密码方式建立 SSH 连接。
     *
     * <p>失败时：会把异常映射为稳定错误码（{@link SshErrorRegistry#errorCodeFrom(Exception)}）并返回给前端。</p>
     */
    @Override
    public void initConnectionByPassword(WebSocketSession session, String ip, int port, String username, String password) {
        JSch jSch = new JSch();

        try {
            Session sshSession = jSch.getSession(username, ip, port);
            sshSession.setPassword(password);
            connectAndStartShell(session, sshSession);
        } catch (Exception e) {
            String code = SshErrorRegistry.errorCodeFrom(e);
            sendErrorAndClose(session, code, SshErrorRegistry.userMessageOf(code));
        }
    }

    /**
     * 用户名 + 私钥方式建立 SSH 连接。
     *
     * <p>注意：这里支持“前端粘贴私钥内容”这种方式，因此使用 JSch 的 addIdentity(byte[]...) 从内存加载密钥，
     * 而不是依赖服务器本地文件。</p>
     *
     * @param privateKeyPem 前端传入的 PEM 格式私钥文本（包含 BEGIN/END 行）
     * @param passphrase   私钥口令，可为空
     */
    @Override
    public void initConnectionByPrivateKey(WebSocketSession session,
                                          String ip,
                                          int port,
                                          String username,
                                          String privateKeyPem,
                                          String passphrase) {
        JSch jSch = new JSch();

        try {
            // JSch 支持从内存加载私钥；passphrase 可为空
            byte[] prvKey = privateKeyPem == null ? null : privateKeyPem.getBytes(StandardCharsets.UTF_8);
            byte[] pph = (passphrase == null || passphrase.isBlank()) ? null : passphrase.getBytes(StandardCharsets.UTF_8);

            // identityName 仅用于标识该密钥，便于排查问题
            jSch.addIdentity("ws-key-" + session.getId(), prvKey, null, pph);

            Session sshSession = jSch.getSession(username, ip, port);
            connectAndStartShell(session, sshSession);
        } catch (Exception e) {
            String code = SshErrorRegistry.errorCodeFrom(e);
            sendErrorAndClose(session, code, SshErrorRegistry.userMessageOf(code));
        }
    }

    /**
     * 建立 SSH Session 并启动 Shell 通道，然后开启异步读取线程把回显推送给前端。
     */
    private void connectAndStartShell(WebSocketSession session, Session sshSession) throws JSchException, IOException {
        // 连接属性：禁用严格 known_hosts 校验。
        // 说明：这样做能减少首次连接交互，但会降低安全性；生产环境建议考虑启用主机指纹校验。
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        sshSession.setConfig(config);

        // 1) 建立 SSH Session（TCP + SSH 握手）
        sshSession.connect(5000);

        // 2) 打开 Shell 通道（交互式终端）
        Channel channel = sshSession.openChannel("shell");
        ChannelShell channelShell = (ChannelShell) channel;
        channelShell.connect(3000);

        // 3) 保存连接信息，后续 command 消消息会根据 WebSocket sessionId 找到对应的 SSH 通道
        SshConnection sshConnection = new SshConnection();
        sshConnection.setChannel(channelShell);
        sshConnection.setJSchSession(sshSession);
        sshMap.put(session.getId(), sshConnection);

        // 4) 告知前端已连接成功并进入“可输入”状态
        // 前端应该监听该消息，将终端 UI 切换为可交互（connected=true 等）
        sendJson(session, "{\"type\":\"ready\"}");

        // 5) 启动线程：持续读取远端回显并推送 WebSocket
        executorService.execute(() -> startReadLoop(session, channelShell));
    }

    /**
     * 循环读取 SSH Shell 的输出流。
     *
     * <p>当远端关闭通道或发生异常时，会退出循环，并触发资源释放。</p>
     */
    private void startReadLoop(WebSocketSession session, ChannelShell channelShell) {
        try {
            InputStream inputStream = channelShell.getInputStream();
            byte[] buffer = new byte[1024];
            int i;

            // 阻塞读取：有输出才会返回
            while ((i = inputStream.read(buffer)) != -1) {
                String msg = new String(buffer, 0, i, StandardCharsets.UTF_8);
                if (session.isOpen()) {
                    // 注意：这里直接发送纯文本回显（不是 JSON）
                    // 终端组件通常需要原样输出（含 ANSI 控制符）
                    session.sendMessage(new TextMessage(msg));
                }
            }
        } catch (Exception e) {
            logger.error("SSH 读取流异常", e);
            close(session);
        }
    }

    /**
     * 接收前端输入并写入 SSH Shell 的输入流。
     *
     * @param command 通常包含用户按键、粘贴内容以及回车(\n)。
     */
    @Override
    public void recvClientCommand(WebSocketSession session, String command) {
        SshConnection connection = sshMap.get(session.getId());
        if (connection != null) {
            try {
                // 把前端输入写入 SSH 的 stdin：客户端命令 -> 远程服务器
                OutputStream outputStream = connection.getChannel().getOutputStream();
                outputStream.write(command.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                logger.error("发送指令失败", e);
                close(session);
            }
        }
    }

    /**
     * 关闭并清理本次 WebSSH 的 SSH 资源。
     *
     * <p>该方法在以下场景会被调用：</p>
     * <ul>
     *   <li>WebSocket 正常断开</li>
     *   <li>WebSocket 传输异常</li>
     *   <li>读取 SSH 流异常</li>
     * </ul>
     */
    @Override
    public void close(WebSocketSession session) {
        SshConnection connection = sshMap.remove(session.getId());
        if (connection != null) {
            if (connection.getChannel() != null) connection.getChannel().disconnect();
            if (connection.getJSchSession() != null) connection.getJSchSession().disconnect();
        }
    }

    /**
     * 发送错误并关闭 WebSocket。
     *
     * <p>前端应消费该错误消息进行精确提示，然后终止终端会话。</p>
     */
    private void sendErrorAndClose(WebSocketSession session, String code, String message) {
        logger.warn("SSH 连接失败: {} - {}", code, message);
        try {
            sendJson(session, "{\"type\":\"error\",\"code\":\"" + escapeJson(code) + "\",\"message\":\"" + escapeJson(message) + "\"}");
        } catch (Exception ignored) {
            // 避免发送错误消息失败导致进一步异常
        }
        try {
            session.close();
        } catch (IOException ignored) {
            // ignore
        }
    }

    /**
     * 发送一段 JSON 文本到前端。
     */
    private void sendJson(WebSocketSession session, String json) throws IOException {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(json));
        }
    }

    /**
     * 最小化 JSON 字符串转义，避免拼接 JSON 时破坏格式。
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    /**
     * 封装单个 WebSSH 会话对应的 JSch 对象。
     */
    private static class SshConnection {
        /** SSH Session（负责握手、认证、加密通道等） */
        private Session jSchSession;

        /** Shell 通道：交互式终端通道 */
        private ChannelShell channel;

        public Session getJSchSession() {
            return jSchSession;
        }

        public void setJSchSession(Session jSchSession) {
            this.jSchSession = jSchSession;
        }

        public ChannelShell getChannel() {
            return channel;
        }

        public void setChannel(ChannelShell channel) {
            this.channel = channel;
        }
    }
}

