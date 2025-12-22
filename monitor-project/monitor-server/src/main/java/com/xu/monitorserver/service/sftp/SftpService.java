package com.xu.monitorserver.service.sftp;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.Session;
import com.xu.monitorserver.dto.sftp.SftpListItem;
import com.xu.monitorserver.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;

@Service
public class SftpService {

    private static final int CONNECT_TIMEOUT_MS = 10_000;

    public List<SftpListItem> list(SftpTicket ticket, String path) {
        String normalized = normalizePath(path);
        try (SftpClient client = openSftp(ticket)) {
            Vector<ChannelSftp.LsEntry> entries = client.sftp.ls(normalized);
            List<SftpListItem> items = new ArrayList<>();
            if (entries == null) return items;

            for (ChannelSftp.LsEntry entry : entries) {
                if (entry == null) continue;

                String name = entry.getFilename();
                if (name == null || name.isBlank() || ".".equals(name) || "..".equals(name)) {
                    continue;
                }

                SftpListItem item = new SftpListItem();
                item.setName(name);
                item.setPath(join(normalized, name));

                SftpATTRS attrs = entry.getAttrs();
                if (attrs != null) {
                    item.setSize(attrs.getSize());
                    item.setMtime(Instant.ofEpochSecond(attrs.getMTime()));
                    item.setPermissions(attrs.getPermissionsString());

                    if (attrs.isDir()) item.setType(SftpListItem.Type.DIR);
                    else if (attrs.isLink()) item.setType(SftpListItem.Type.LINK);
                    else if (attrs.isReg()) item.setType(SftpListItem.Type.FILE);
                    else item.setType(SftpListItem.Type.OTHER);
                } else {
                    item.setType(SftpListItem.Type.OTHER);
                }

                items.add(item);
            }

            // 目录优先 + 名称排序
            items.sort(Comparator
                    .comparing((SftpListItem i) -> i.getType() != SftpListItem.Type.DIR)
                    .thenComparing(SftpListItem::getName, String.CASE_INSENSITIVE_ORDER));

            return items;
        } catch (Exception e) {
            String code = SftpErrorRegistry.errorCodeFrom(asException(e));
            throw new ServiceException(500, SftpErrorRegistry.userMessageOf(code));
        }
    }

    public void downloadToStream(SftpTicket ticket, String remotePath, OutputStream out) {
        String normalized = normalizePath(remotePath);
        try (SftpClient client = openSftp(ticket)) {
            client.sftp.get(normalized, out);
        } catch (Exception e) {
            String code = SftpErrorRegistry.errorCodeFrom(asException(e));
            throw new ServiceException(500, SftpErrorRegistry.userMessageOf(code));
        }
    }

    public SftpUploadResult uploadFromStream(SftpTicket ticket,
                                            String targetDir,
                                            String fileName,
                                            InputStream in,
                                            boolean overwrite) {
        String dir = normalizePath(targetDir);
        String safeName = sanitizeFileName(fileName);
        String remotePath = join(dir, safeName);

        try (SftpClient client = openSftp(ticket)) {
            if (!overwrite) {
                // 若文件存在则拒绝
                try {
                    client.sftp.stat(remotePath);
                    throw new ServiceException(409, "文件已存在，禁止覆盖");
                } catch (SftpException ignore) {
                    // 不存在则 OK
                }
            }

            client.sftp.put(in, remotePath);

            Long size = null;
            try {
                SftpATTRS attrs = client.sftp.stat(remotePath);
                if (attrs != null) size = attrs.getSize();
            } catch (Exception ignored) {
            }

            return new SftpUploadResult(remotePath, size);
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            String code = SftpErrorRegistry.errorCodeFrom(asException(e));
            throw new ServiceException(500, SftpErrorRegistry.userMessageOf(code));
        }
    }

    private SftpClient openSftp(SftpTicket ticket) throws Exception {
        if (ticket == null) {
            throw new ServiceException(401, SftpErrorRegistry.userMessageOf(SftpErrorRegistry.SFTP_TICKET_INVALID));
        }

        JSch jsch = new JSch();

        if ("publicKey".equalsIgnoreCase(ticket.getAuthType())) {
            byte[] prvKey = ticket.getPrivateKeyPem() == null
                    ? null
                    : ticket.getPrivateKeyPem().getBytes(StandardCharsets.UTF_8);
            byte[] pph = (ticket.getPassphrase() == null || ticket.getPassphrase().isBlank())
                    ? null
                    : ticket.getPassphrase().getBytes(StandardCharsets.UTF_8);
            jsch.addIdentity("sftp-" + UUID.randomUUID(), prvKey, null, pph);
        }

        Session session = jsch.getSession(ticket.getSshUsername(), ticket.getHost(), ticket.getPort());
        if (!"publicKey".equalsIgnoreCase(ticket.getAuthType())) {
            session.setPassword(ticket.getPassword());
        }

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(CONNECT_TIMEOUT_MS);

        Channel channel = session.openChannel("sftp");
        channel.connect(CONNECT_TIMEOUT_MS);

        return new SftpClient(session, (ChannelSftp) channel);
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new ServiceException(400, "文件名不能为空");
        }
        String name = fileName.replace('\\', '/');
        int idx = name.lastIndexOf('/');
        if (idx >= 0) name = name.substring(idx + 1);
        name = name.trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            throw new ServiceException(400, "文件名不合法");
        }
        return name;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        String p = path.trim().replace('\\', '/');
        if (p.contains("..")) {
            throw new ServiceException(400, "路径不合法");
        }
        while (p.contains("//")) {
            p = p.replace("//", "/");
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private static String join(String dir, String name) {
        if (dir == null || dir.isBlank() || "/".equals(dir)) {
            return "/" + name;
        }
        if (dir.endsWith("/")) {
            return dir + name;
        }
        return dir + "/" + name;
    }

    private static Exception asException(Throwable t) {
        return (t instanceof Exception e) ? e : new RuntimeException(t);
    }

    private static final class SftpClient implements AutoCloseable {
        private final Session session;
        private final ChannelSftp sftp;

        private SftpClient(Session session, ChannelSftp sftp) {
            this.session = session;
            this.sftp = sftp;
        }

        @Override
        public void close() {
            try {
                if (sftp != null) sftp.disconnect();
            } catch (Exception ignored) {
            }
            try {
                if (session != null) session.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    public record SftpUploadResult(String remotePath, Long size) {
    }
}
