package com.xu.monitorserver.dto.sftp;

import java.time.Instant;

public class SftpListItem {

    public enum Type {
        FILE, DIR, LINK, OTHER
    }

    private String name;
    private String path;
    private Type type;
    private Long size;
    private Instant mtime;
    private String permissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Instant getMtime() {
        return mtime;
    }

    public void setMtime(Instant mtime) {
        this.mtime = mtime;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
