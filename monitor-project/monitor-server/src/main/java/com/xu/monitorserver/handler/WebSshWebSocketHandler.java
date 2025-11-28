package com.xu.monitorserver.handler;


import com.xu.monitorserver.service.SshService.SshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;

@Component
public class WebSshWebSocketHandler implements WebSocketHandler {

    @Autowired
    private SshService sshService;

    /**
     * 连接建立后触发
     * 前端 URL: ws://localhost:8080/ws/ssh?ip=x.x.x.x&user=root&pwd=123
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 解析 URL 中的参数 (IP, 用户名, 密码)
        URI uri = session.getUri();
        String query = uri.getQuery(); // ip=...&user=...
        // 简单粗暴的解析方式 (实际项目建议用工具类)
        String[] params = query.split("&");
        String ip = null, user = null, pwd = null;
        for (String param : params) {
            String[] kv = param.split("=");
            if ("ip".equals(kv[0])) ip = kv[1];
            if ("user".equals(kv[0])) user = kv[1];
            if ("pwd".equals(kv[0])) pwd = kv[1];
        }

        if (ip != null && user != null && pwd != null) {
            // 2. 调用 Service 开启 SSH 连接
            sshService.initConnection(session, ip, 22, user, pwd);
        } else {
            session.sendMessage(new TextMessage("Error: 参数缺失"));
            session.close();
        }
    }

    /**
     * 收到前端消息时触发 (用户按键)
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            // 调用 Service 发送命令
            sshService.recvClientCommand(session, ((TextMessage) message).getPayload());
        }
    }

    /**
     * 连接关闭时触发
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        sshService.close(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sshService.close(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}