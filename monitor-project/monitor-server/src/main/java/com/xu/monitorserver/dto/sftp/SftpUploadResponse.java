package com.xu.monitorserver.dto.sftp;

public class SftpUploadResponse {

    private String remotePath;
    private Long size;

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
