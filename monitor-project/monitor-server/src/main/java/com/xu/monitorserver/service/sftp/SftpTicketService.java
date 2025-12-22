package com.xu.monitorserver.service.sftp;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SFTP Ticket 服务：负责签发与一次性消费 ticket。
 *
 * <p>与 WebSSH 的 {@code SshTicketService} 一致：
 * ticket 短期有效、一次性消费、与 username 绑定。
 * 当前实现使用 JVM 内存缓存；若未来多实例部署建议迁移到 Redis 并使用 getdel 保证原子性。</p>
 */
@Service
public class SftpTicketService {

    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, TicketRecord> cache = new ConcurrentHashMap<>();

    public String issue(String ownerUsername, SftpTicket ticket) {
        String token = generateToken();
        cache.put(token, new TicketRecord(ownerUsername, ticket));
        return token;
    }

    public SftpTicket consume(String ownerUsername, String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        TicketRecord record = cache.remove(token);
        if (record == null) {
            return null;
        }
        if (record.ticket == null || record.ticket.isExpired()) {
            return null;
        }
        if (record.ownerUsername == null || !record.ownerUsername.equals(ownerUsername)) {
            return null;
        }
        return record.ticket;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return raw + "." + Instant.now().toEpochMilli();
    }

    private record TicketRecord(String ownerUsername, SftpTicket ticket) {
    }
}

