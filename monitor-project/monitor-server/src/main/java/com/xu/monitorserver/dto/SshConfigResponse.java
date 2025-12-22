package com.xu.monitorserver.dto;

/**
 * /api/server/{id}/ssh-config 接口响应。
 *
 * <p>注意：该响应不包含 password/privateKey/passphrase 明文。
 * 前端拿到 sshTicket 后，在 WebSocket connect 报文里携带 ticket 即可触发服务端侧连接。</p>
 */
public class SshConfigResponse {

    private Long serverId;
    private String host;
    private Integer port;
    private String username;

    /** publicKey / password（后端根据是否配置私钥自动给出推荐值） */
    private String authPreferred;

    private Boolean hasPrivateKey;
    private String fingerprint;

    /** 一次性 ticket，短期有效 */
    private String sshTicket;

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAuthPreferred() {
        return authPreferred;
    }

    public void setAuthPreferred(String authPreferred) {
        this.authPreferred = authPreferred;
    }

    public Boolean getHasPrivateKey() {
        return hasPrivateKey;
    }

    public void setHasPrivateKey(Boolean hasPrivateKey) {
        this.hasPrivateKey = hasPrivateKey;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getSshTicket() {
        return sshTicket;
    }

    public void setSshTicket(String sshTicket) {
        this.sshTicket = sshTicket;
    }
}

