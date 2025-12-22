package com.xu.monitorserver.service.sshservice;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSSH Ticket 服务：负责签发与一次性消费 ticket。
 *
 * <p>实现目标：
 * <ul>
 *   <li>前端 REST 获取 ssh-config 时拿到 ticket，但拿不到私钥/密码明文</li>
 *   <li>WebSocket connect 时携带 ticket，服务端再取出敏感信息发起 SSH</li>
 *   <li>ticket 短时有效、一次性消费、可按用户名绑定</li>
 * </ul>
 */
@Service
public class SshTicketService {

    /** 默认 ticket 有效期：60 秒 */
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 内存缓存：ticket -> payload。
     *
     * <p>说明：
     * 目前使用 JVM 内存即可；如果未来 monitor-server 水平扩展为多实例，
     * 建议改为 Redis（并设置短 TTL + 原子 getdel）。
     */
    private final Map<String, TicketRecord> cache = new ConcurrentHashMap<>();

    /**
     * 签发 ticket。
     *
     * @param ownerUsername 当前登录用户（用于越权防护）
     */
    public String issue(String ownerUsername, SshTicket ticket) {
        String token = generateToken();
        cache.put(token, new TicketRecord(ownerUsername, ticket));
        return token;
    }

    /**
     * 一次性消费 ticket。
     *
     * <p>消费成功后会从缓存中移除，避免重复使用。</p>
     */
    public SshTicket consume(String ownerUsername, String token) {
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
            // 越权：ticket 不属于当前用户
            return null;
        }
        return record.ticket;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        // URL-safe Base64，避免在 URL/JSON 里出现 + / =
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // 多拼一段时间戳，降低同机并发碰撞概率（虽然后者也极低）
        return raw + "." + Instant.now().toEpochMilli();
    }

    private record TicketRecord(String ownerUsername, SshTicket ticket) {
    }
}

