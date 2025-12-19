package com.xu.monitorserver.service.sshservice;

import com.jcraft.jsch.JSchException;

/**
 * WebSSH 错误码与用户可读提示的集中管理。
 *
 * <p>目的：避免在业务代码里散落硬编码字符串，统一前后端展示口径，便于后续国际化/配置化。</p>
 */
public final class SshErrorRegistry {

    private SshErrorRegistry() {
    }

    /** 协议/参数错误 */
    public static final String WS_BAD_REQUEST = "WS_BAD_REQUEST";
    public static final String SSH_CONNECT_PAYLOAD_INVALID = "SSH_CONNECT_PAYLOAD_INVALID";

    /** SSH 错误 */
    public static final String SSH_AUTH_FAILED = "SSH_AUTH_FAILED";
    public static final String SSH_KEY_INVALID = "SSH_KEY_INVALID";
    public static final String SSH_HOST_UNREACHABLE = "SSH_HOST_UNREACHABLE";
    public static final String SSH_CONNECT_FAILED = "SSH_CONNECT_FAILED";
    public static final String SSH_INTERNAL_ERROR = "SSH_INTERNAL_ERROR";

    /**
     * 根据异常映射成稳定的错误码（前端可据此做精确提示/埋点）。
     */
    public static String errorCodeFrom(Exception e) {
        String msg = e != null && e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        // JSch 常见：Auth fail
        if (msg.contains("auth fail") || msg.contains("authentication") || msg.contains("auth cancel")) {
            return SSH_AUTH_FAILED;
        }

        // 私钥解析问题（mwiede jsch 常见信息包含 invalid privatekey / unknown key type）
        if (msg.contains("invalid privatekey")
                || msg.contains("invalid private key")
                || msg.contains("unknown key type")
                || msg.contains("invalid format")) {
            return SSH_KEY_INVALID;
        }

        if (msg.contains("unknownhostexception") || msg.contains("unknown host")) {
            return SSH_HOST_UNREACHABLE;
        }

        if (msg.contains("timeout") || msg.contains("timed out") || msg.contains("sockettimeout")) {
            return SSH_HOST_UNREACHABLE;
        }

        if (msg.contains("refused")) {
            return SSH_HOST_UNREACHABLE;
        }

        if (e instanceof JSchException) {
            return SSH_CONNECT_FAILED;
        }

        return SSH_INTERNAL_ERROR;
    }

    /**
     * 获取用户可读的提示文案（尽量不暴露底层异常细节）。
     */
    public static String userMessageOf(String code) {
        if (code == null) return "连接失败：服务端异常";

        return switch (code) {
            case SSH_AUTH_FAILED -> "认证失败：请检查用户名/密码或密钥口令是否正确";
            case SSH_KEY_INVALID -> "私钥无效：请粘贴正确的 PEM 格式私钥（BEGIN/END 行必须完整）";
            case SSH_HOST_UNREACHABLE -> "连接失败：目标主机不可达或端口未开放";
            case SSH_CONNECT_FAILED -> "连接失败：无法建立 SSH 会话";
            case SSH_CONNECT_PAYLOAD_INVALID -> "参数错误：请检查连接信息";
            case WS_BAD_REQUEST -> "消息解析失败：请检查参数格式";
            default -> "连接失败：服务端异常";
        };
    }
}

