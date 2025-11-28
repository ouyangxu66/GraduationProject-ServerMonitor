package com.xu.monitorserver.service.SshService;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SshServiceImpl implements SshService {

    private static final Logger logger = LoggerFactory.getLogger(SshServiceImpl.class);

    // 存放所有的 SSH 连接会话，Key 是 WebSocket 的 SessionId
    private static final Map<String, SshConnection> sshMap = new ConcurrentHashMap<>();

    // 线程池，用于异步读取 SSH 的返回结果
    private final ExecutorService executorService = Executors.newCachedThreadPool();


    @Override
    public void initConnection(WebSocketSession session, String ip, int port, String username, String password) {
        JSch jSch = new JSch();
        SshConnection sshConnection = new SshConnection();

        try {
            // 1. 建立 SSH Session
            Session sshSession = jSch.getSession(username, ip, port);
            sshSession.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // 不验证指纹，省事
            sshSession.setConfig(config);
            sshSession.connect(3000); // 3秒超时

            // 2. 打开 Shell 通道 (像打开一个终端窗口)
            Channel channel = sshSession.openChannel("shell");
            ChannelShell channelShell = (ChannelShell) channel;
            channelShell.connect(3000);

            // 3. 保存连接信息
            sshConnection.setChannel(channelShell);
            sshConnection.setJSchSession(sshSession);
            sshMap.put(session.getId(), sshConnection);

            // 4. 启动一个线程，专门负责“监听 Linux 的回话”并推给前端
            executorService.execute(() -> {
                try {
                    InputStream inputStream = channelShell.getInputStream();
                    byte[] buffer = new byte[1024];
                    int i;
                    // 循环读取 Linux 的输出
                    while ((i = inputStream.read(buffer)) != -1) {
                        String msg = new String(buffer, 0, i);
                        // 通过 WebSocket 发回前端
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(msg));
                        }
                    }
                } catch (Exception e) {
                    logger.error("SSH 读取流异常", e);
                    close(session);
                }
            });

        } catch (Exception e) {
            logger.error("SSH 连接失败", e);
            try {
                // 告诉前端连接失败
                session.sendMessage(new TextMessage("Error: 连接失败 - " + e.getMessage()));
                session.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void recvClientCommand(WebSocketSession session, String command) {
        SshConnection connection = sshMap.get(session.getId());
        if (connection != null) {
            try {
                // 把前端的按键写入 SSH 的输入流
                OutputStream outputStream = connection.getChannel().getOutputStream();
                outputStream.write(command.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                logger.error("发送指令失败", e);
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

    // 内部类，用于封装 SSH 连接对象
    private static class SshConnection {
        private Session jSchSession;
        private ChannelShell channel;

        // Getters and Setters (可以使用 Lombok @Data)
        public Session getJSchSession() { return jSchSession; }
        public void setJSchSession(Session jSchSession) { this.jSchSession = jSchSession; }
        public ChannelShell getChannel() { return channel; }
        public void setChannel(ChannelShell channel) { this.channel = channel; }
    }
}