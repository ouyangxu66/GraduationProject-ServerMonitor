package com.xu.monitorserver.service.sftp;

import java.time.Instant;

/**
 * SFTP 一次性票据（Ticket）。
 *
 * <p>用于“前端不下发明文 ssh 密码/私钥”的场景：前端先通过 REST 获取短期 ticket，
 * 再用 ticket 调用 list/upload/download 等文件接口。
 * ticket 仅短期有效 + 只能消费一次 + 与 username 绑定，防止被重放/越权使用。</p>
 */
public class SftpTicket {

    private final Long serverId;
    private final String host;
    private final int port;
    private final String sshUsername;

    /** publicKey / password */
    private final String authType;

    /** 明文密码（仅内存，可能为空） */
    private final String password;

    /** 明文私钥 PEM（仅内存，可能为空） */
    private final String privateKeyPem;

    /** 明文 passphrase（仅内存，可能为空） */
    private final String passphrase;

    private final Instant expireAt;

    public SftpTicket(Long serverId,
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

    public boolean isExpired() {
        return expireAt != null && Instant.now().isAfter(expireAt);
    }
}
