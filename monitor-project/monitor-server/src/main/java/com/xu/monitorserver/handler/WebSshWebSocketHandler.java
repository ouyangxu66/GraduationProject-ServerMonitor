package com.xu.monitorserver.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xu.monitorserver.service.sshservice.ISshService;
import com.xu.monitorserver.service.sshservice.SshErrorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;

@Component
public class WebSshWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSshWebSocketHandler.class);

    private final ISshService sshService;

    public WebSshWebSocketHandler(ISshService sshService) {
        this.sshService = sshService;
    }

    // Jackson JSON 解析工具
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立后触发
     * 此时前端只是连上了 WebSocket，还未传递 SSH 目标信息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("WebSSH WebSocket 连接建立: {}", session.getId());
    }

    /**
     * 收到前端消息时触发
     * 消息格式约定为 JSON: { "operate": "connect|command", ... }
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        if (!(message instanceof TextMessage)) {
            return;
        }

        String payload = ((TextMessage) message).getPayload();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String operate = (String) data.get("operate");

            if ("connect".equals(operate)) {
                handleConnect(session, data);
                return;
            }

            if ("command".equals(operate)) {
                String command = (String) data.get("command");
                if (command != null) {
                    sshService.recvClientCommand(session, command);
                }
                return;
            }

            sendError(session, SshErrorRegistry.WS_BAD_REQUEST, "不支持的操作类型: " + operate);
        } catch (Exception e) {
            logger.error("消息解析失败", e);
            sendError(session, SshErrorRegistry.WS_BAD_REQUEST, SshErrorRegistry.userMessageOf(SshErrorRegistry.WS_BAD_REQUEST));
        }
    }

    private void handleConnect(WebSocketSession session, Map<String, Object> data) {
        String host = (String) data.get("host");
        Integer port = data.get("port") != null ? Integer.valueOf(String.valueOf(data.get("port"))) : 22;
        String username = (String) data.get("username");

        String authType = data.get("authType") != null ? String.valueOf(data.get("authType")) : "password";

        if (host == null || host.isBlank()) {
            sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：请填写主机 IP");
            return;
        }
        if (username == null || username.isBlank()) {
            sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：请填写用户名");
            return;
        }

        if ("password".equalsIgnoreCase(authType)) {
            String password = (String) data.get("password");
            if (password == null || password.isBlank()) {
                sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：请填写密码");
                return;
            }
            sshService.initConnectionByPassword(session, host, port, username, password);
            return;
        }

        if ("publicKey".equalsIgnoreCase(authType) || "key".equalsIgnoreCase(authType)) {
            String privateKey = (String) data.get("privateKey");
            String passphrase = (String) data.get("passphrase");

            if (privateKey == null || privateKey.isBlank()) {
                sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：请粘贴私钥内容");
                return;
            }
            sshService.initConnectionByPrivateKey(session, host, port, username, privateKey, passphrase);
            return;
        }

        sendError(session, SshErrorRegistry.SSH_CONNECT_PAYLOAD_INVALID, "参数错误：不支持的认证方式 " + authType);
    }

    private void sendError(WebSocketSession session, String code, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage("{\"type\":\"error\",\"code\":\"" + escapeJson(code) + "\",\"message\":\"" + escapeJson(message) + "\"}"));
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket 传输异常", exception);
        sshService.close(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        logger.info("WebSSH 连接断开: {}", session.getId());
        sshService.close(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}