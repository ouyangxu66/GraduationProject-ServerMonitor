package com.xu.monitorserver.service.sshservice;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SshServiceImpl implements ISshService {

    private static final Logger logger = LoggerFactory.getLogger(SshServiceImpl.class);

    // 存放所有的 SSH 连接会话，Key 是 WebSocket 的 SessionId
    private static final Map<String, SshConnection> sshMap = new ConcurrentHashMap<>();

    // 线程池，用于异步读取 SSH 的返回结果
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void initConnection(WebSocketSession session, String ip, int port, String username, String password) {
        // 兼容旧接口：默认走密码模式
        initConnectionByPassword(session, ip, port, username, password);
    }

    @Override
    public void initConnectionByPassword(WebSocketSession session, String ip, int port, String username, String password) {
        JSch jSch = new JSch();

        try {
            Session sshSession = jSch.getSession(username, ip, port);
            sshSession.setPassword(password);
            connectAndStartShell(session, sshSession);
        } catch (Exception e) {
            String code = SshErrorRegistry.errorCodeFrom(e);
            sendErrorAndClose(session, code, SshErrorRegistry.userMessageOf(code));
        }
    }

    @Override
    public void initConnectionByPrivateKey(WebSocketSession session,
                                          String ip,
                                          int port,
                                          String username,
                                          String privateKeyPem,
                                          String passphrase) {
        JSch jSch = new JSch();

        try {
            // JSch 支持从内存加载私钥；passphrase 可为空
            byte[] prvKey = privateKeyPem == null ? null : privateKeyPem.getBytes(StandardCharsets.UTF_8);
            byte[] pph = (passphrase == null || passphrase.isBlank()) ? null : passphrase.getBytes(StandardCharsets.UTF_8);
            jSch.addIdentity("ws-key-" + session.getId(), prvKey, null, pph);

            Session sshSession = jSch.getSession(username, ip, port);
            connectAndStartShell(session, sshSession);
        } catch (Exception e) {
            String code = SshErrorRegistry.errorCodeFrom(e);
            sendErrorAndClose(session, code, SshErrorRegistry.userMessageOf(code));
        }
    }

    private void connectAndStartShell(WebSocketSession session, Session sshSession) throws JSchException, IOException {
        //设置SSH连接属性，禁用严格的主机密钥检查
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        sshSession.setConfig(config);

        // 建立 SSH Session
        sshSession.connect(5000);

        // 打开 Shell 通道 (像打开一个终端窗口)
        Channel channel = sshSession.openChannel("shell");
        ChannelShell channelShell = (ChannelShell) channel;
        channelShell.connect(3000);

        // 保存连接信息
        SshConnection sshConnection = new SshConnection();
        sshConnection.setChannel(channelShell);
        sshConnection.setJSchSession(sshSession);
        sshMap.put(session.getId(), sshConnection);

        // 通知前端：已就绪（前端应据此判定 connected=true）
        sendJson(session, "{\"type\":\"ready\"}");

        // 启动线程：监听远端回显并推送给前端
        executorService.execute(() -> startReadLoop(session, channelShell));
    }

    private void startReadLoop(WebSocketSession session, ChannelShell channelShell) {
        try {
            InputStream inputStream = channelShell.getInputStream();
            byte[] buffer = new byte[1024];
            int i;
            while ((i = inputStream.read(buffer)) != -1) {
                String msg = new String(buffer, 0, i, StandardCharsets.UTF_8);
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(msg));
                }
            }
        } catch (Exception e) {
            logger.error("SSH 读取流异常", e);
            close(session);
        }
    }

    @Override
    public void recvClientCommand(WebSocketSession session, String command) {
        SshConnection connection = sshMap.get(session.getId());
        if (connection != null) {
            try {
                // 把前端的按键写入 SSH 的输入流,客户端命令->远程服务器
                OutputStream outputStream = connection.getChannel().getOutputStream();
                outputStream.write(command.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                logger.error("发送指令失败", e);
                close(session);
            }
        }
    }

    @Override
    public void close(WebSocketSession session) {
        SshConnection connection = sshMap.remove(session.getId());
        if (connection != null) {
            if (connection.getChannel() != null) connection.getChannel().disconnect();
            if (connection.getJSchSession() != null) connection.getJSchSession().disconnect();
        }
    }

    private void sendErrorAndClose(WebSocketSession session, String code, String message) {
        logger.warn("SSH 连接失败: {} - {}", code, message);
        try {
            sendJson(session, "{\"type\":\"error\",\"code\":\"" + escapeJson(code) + "\",\"message\":\"" + escapeJson(message) + "\"}");
        } catch (Exception ignored) {
            // ignore
        }
        try {
            session.close();
        } catch (IOException ignored) {
            // ignore
        }
    }

    private void sendJson(WebSocketSession session, String json) throws IOException {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(json));
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    // 内部类，封装 SSH 连接对象,用于后端连接服务器
    private static class SshConnection {
        private Session jSchSession;
        private ChannelShell channel;

        public Session getJSchSession() {
            return jSchSession;
        }

        public void setJSchSession(Session jSchSession) {
            this.jSchSession = jSchSession;
        }

        public ChannelShell getChannel() {
            return channel;
        }

        public void setChannel(ChannelShell channel) {
            this.channel = channel;
        }
    }
}

