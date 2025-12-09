package com.xu.monitorserver.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xu.monitorserver.service.sshservice.SshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;

@Component
public class WebSshWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSshWebSocketHandler.class);

    private SshService sshService;

    public WebSshWebSocketHandler(SshService sshService){
        this.sshService=sshService;
    }

    // Jackson JSON 解析工具
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立后触发
     * 此时前端只是连上了 WebSocket，还未传递 SSH 目标信息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSSH WebSocket 连接建立: {}", session.getId());
        // 🔴 这里不能建立 SSH 连接，因为还不知道要连哪台服务器
        // 等待前端发送第一条 JSON 消息
    }

    /**
     * 收到前端消息时触发
     * 消息格式约定为 JSON: { "operate": "connect|command", ... }
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();

            try {
                // 1. 解析 JSON
                // 建议使用 Map 接收，或者你之前定义的 WebSshData 实体类
                Map<String, Object> data = objectMapper.readValue(payload, Map.class);

                // 2. 获取操作类型
                String operate = (String) data.get("operate");

                if ("connect".equals(operate)) {
                    // 🟢 情况 A：建立 SSH 连接请求
                    String host = (String) data.get("host");
                    // 防止 port 为空或类型转换错误
                    Integer port = data.get("port") != null ? Integer.valueOf(String.valueOf(data.get("port"))) : 22;
                    String username = (String) data.get("username");
                    String password = (String) data.get("password");

                    // 调用 Service
                    sshService.initConnection(session, host, port, username, password);

                } else if ("command".equals(operate)) {
                    // 🟢 情况 B：发送终端命令
                    String command = (String) data.get("command");
                    sshService.recvClientCommand(session, command);
                }
            } catch (Exception e) {
                logger.error("消息解析失败", e);
                // 可以在这里给前端回传一个错误提示
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket 传输异常", exception);
        sshService.close(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSSH 连接断开: {}", session.getId());
        sshService.close(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}