package com.xu.monitorserver.service.sshservice;

import org.springframework.web.socket.WebSocketSession;

public interface ISshService {

    /**
     * 初始化 SSH 连接（兼容旧版：用户名 + 密码）
     *
     * @param session WebSocket 会话 (用于把结果推回前端)
     * @param ip      目标服务器 IP
     * @param port    SSH 端口 (默认22)
     * @param username 用户名
     * @param password 密码
     */
    void initConnection(WebSocketSession session, String ip, int port, String username, String password);

    /**
     * 初始化 SSH 连接：用户名 + 密码
     */
    void initConnectionByPassword(WebSocketSession session, String ip, int port, String username, String password);

    /**
     * 初始化 SSH 连接：用户名 + 私钥 (可选 passphrase)
     *
     * @param privateKeyPem 私钥 PEM 文本（包含 BEGIN/END 行）
     * @param passphrase    私钥口令（可为空）
     */
    void initConnectionByPrivateKey(WebSocketSession session,
                                    String ip,
                                    int port,
                                    String username,
                                    String privateKeyPem,
                                    String passphrase);

    /**
     * 处理前端发来的命令
     *
     * @param session WebSocket 会话
     * @param command 用户输入的字符
     */
    void recvClientCommand(WebSocketSession session, String command);

    /**
     * 关闭连接
     */
    void close(WebSocketSession session);
}
