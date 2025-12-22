package com.xu.monitorserver.service.sftp;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

/**
 * SFTP 错误码映射：把底层异常映射为稳定的 code + 面向用户的 message。
 */
public final class SftpErrorRegistry {

    private SftpErrorRegistry() {
    }

    public static final String SFTP_TICKET_INVALID = "SFTP_TICKET_INVALID";
    public static final String SFTP_CONNECT_FAILED = "SFTP_CONNECT_FAILED";
    public static final String SFTP_PERMISSION_DENIED = "SFTP_PERMISSION_DENIED";
    public static final String SFTP_NOT_FOUND = "SFTP_NOT_FOUND";
    public static final String SFTP_PATH_INVALID = "SFTP_PATH_INVALID";
    public static final String SFTP_IO_ERROR = "SFTP_IO_ERROR";

    public static String errorCodeFrom(Exception e) {
        if (e instanceof SftpException se) {
            // 常见映射
            if (se.id == ChannelSftpFx.SSH_FX_PERMISSION_DENIED) {
                return SFTP_PERMISSION_DENIED;
            }
            if (se.id == ChannelSftpFx.SSH_FX_NO_SUCH_FILE) {
                return SFTP_NOT_FOUND;
            }
            return SFTP_IO_ERROR;
        }
        if (e instanceof JSchException) {
            return SFTP_CONNECT_FAILED;
        }
        return SFTP_IO_ERROR;
    }

    public static String userMessageOf(String code) {
        if (code == null) return "SFTP 操作失败";
        return switch (code) {
            case SFTP_TICKET_INVALID -> "文件操作票据已失效，请刷新后重试";
            case SFTP_CONNECT_FAILED -> "连接服务器失败，请检查网络/账号权限";
            case SFTP_PERMISSION_DENIED -> "权限不足，无法访问该目录或文件";
            case SFTP_NOT_FOUND -> "目录或文件不存在";
            case SFTP_PATH_INVALID -> "路径不合法";
            default -> "SFTP 操作失败";
        };
    }

    /**
     * 复刻 JSch 的 FX 常量，避免直接依赖包可见的实现细节。
     */
    static final class ChannelSftpFx {
        static final int SSH_FX_OK = 0;
        static final int SSH_FX_EOF = 1;
        static final int SSH_FX_NO_SUCH_FILE = 2;
        static final int SSH_FX_PERMISSION_DENIED = 3;
        static final int SSH_FX_FAILURE = 4;

        private ChannelSftpFx() {
        }
    }
}

