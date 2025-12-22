package com.xu.monitorserver.service.sshservice;

import java.time.Instant;

/**
 * WebSSH 一次性票据（Ticket）。
 *
 * <p>用于“前端不下发明文私钥/密码”场景：
 * 前端点击服务器列表的“终端”按钮后，仅携带 serverId 进入终端页。
 * 终端页先请求 /api/server/{id}/ssh-config，后端返回一个短时有效的一次性票据 sshTicket。
 * 前端建立 WebSocket 后发送 connect 报文携带 ticket。
 * WebSocket Handler 再用 ticket 从服务端缓存中取回敏感凭证（已解密的密码/私钥/passphrase），
 * 并执行 JSch 连接。
 *
 * <p>安全性：
 * <ul>
 *   <li>ticket 仅短期有效（例如 60s）</li>
 *   <li>ticket 只能使用一次（consume）</li>
 *   <li>ticket 与 username 绑定，防止越权使用</li>
 * </ul>
 */
public class SshTicket {

    /** ticket 关联的服务器 id */
    private final Long serverId;

    /** 目标主机 */
    private final String host;

    /** SSH 端口 */
    private final int port;

    /** 登陆用户名（Linux 用户名） */
    private final String sshUsername;

    /** 优先认证方式：publicKey 优先，否则 password */
    private final String authType;

    /** 明文密码（仅存于内存，不落盘） */
    private final String password;

    /** 明文私钥 PEM（仅存于内存，不落盘） */
    private final String privateKeyPem;

    /** 明文 passphrase（仅存于内存，不落盘，可为空） */
    private final String passphrase;

    /** 过期时间戳 */
    private final Instant expireAt;

    public SshTicket(Long serverId,
                     String host,
                     int port,
                     String sshUsername,
                     String authType,
                     String password,
                     String privateKeyPem,
                     String passphrase,
                     Instant expireAt) {
        this.serverId = serverId;
        this.host = host;
        this.port = port;
        this.sshUsername = sshUsername;
        this.authType = authType;
        this.password = password;
        this.privateKeyPem = privateKeyPem;
        this.passphrase = passphrase;
        this.expireAt = expireAt;
    }

    public Long getServerId() {
        return serverId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public String getAuthType() {
        return authType;
    }

    public String getPassword() {
        return password;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    /**
     * 判断 ticket 是否已过期。
     */
    public boolean isExpired() {
        return expireAt != null && Instant.now().isAfter(expireAt);
    }
}
