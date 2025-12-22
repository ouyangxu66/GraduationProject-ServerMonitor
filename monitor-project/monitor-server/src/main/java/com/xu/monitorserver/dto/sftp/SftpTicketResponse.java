package com.xu.monitorserver.dto.sftp;

import java.time.Instant;

public class SftpTicketResponse {

    private Long serverId;
    private String sftpTicket;
    private Instant expiresAt;

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public String getSftpTicket() {
        return sftpTicket;
    }

    public void setSftpTicket(String sftpTicket) {
        this.sftpTicket = sftpTicket;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
