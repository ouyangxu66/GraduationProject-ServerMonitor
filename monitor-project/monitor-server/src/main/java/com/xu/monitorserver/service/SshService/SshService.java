package com.xu.monitorserver.service.SshService;

import org.springframework.web.socket.WebSocketSession;

public interface SshService {

    /**
     * 初始化 SSH 连接
     * @param session WebSocket 会话 (用于把结果推回前端)
     * @param ip 目标服务器 IP
     * @param port SSH 端口 (默认22)
     * @param username 用户名
     * @param password 密码
     */
    void initConnection(WebSocketSession session, String ip, int port, String username, String password);

    /**
     * 处理前端发来的命令
     * @param session WebSocket 会话
     * @param command 用户输入的字符
     */
    void recvClientCommand(WebSocketSession session, String command);

    /**
     * 关闭连接
     */
    void close(WebSocketSession session);
}
