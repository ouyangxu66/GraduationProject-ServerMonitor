package com.xu.monitorserver.service.serverservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 服务器 SSH 凭证加解密服务（用于 password/privateKey/passphrase 的落库加密）。
 *
 * <h2>为什么需要这个类？</h2>
 * <ul>
 *   <li>本需求选择：A 票据(ticket)方案，不下发明文凭证到前端。</li>
 *   <li>因此 password/privateKey/passphrase 必须留在服务端侧使用。</li>
 *   <li>为降低“数据库泄露”风险，推荐将敏感字段在应用层加密后再入库。</li>
 * </ul>
 *
 * <h2>实现策略</h2>
 * <ul>
 *   <li>AES-256-GCM：带认证的对称加密，能检测篡改。</li>
 *   <li>密钥来源：application.yml 的 monitor.crypto.ssh-secret-key（Base64 32 bytes）。</li>
 *   <li>编码格式：Base64( 12字节IV + ciphertext )</li>
 * </ul>
 *
 * <p><b>注意</b>：
 * <ul>
 *   <li>该密钥必须妥善保管；生产环境建议来自环境变量/配置中心/密钥管理服务。</li>
 *   <li>若未来需要“密钥轮换”，建议增加版本号字段并保留旧版本 key。</li>
 * </ul>
 */
@Service
public class SshSecretCryptoService {

    /**
     * AES-GCM 推荐 12 bytes IV。
     */
    private static final int IV_LENGTH = 12;

    /**
     * GCM Tag 长度（bit）。
     */
    private static final int TAG_LENGTH_BIT = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    private final SecretKey secretKey;

    public SshSecretCryptoService(
            @Value("${monitor.crypto.ssh-secret-key:}") String base64Key
    ) {
        if (base64Key == null || base64Key.isBlank()) {
            // 不直接抛异常是为了开发环境更好启动；但会导致无法保存私钥/密码。
            // 业务侧会在 encrypt/decrypt 时给出明确错误。
            this.secretKey = null;
            return;
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密明文字符串为 Base64。
     */
    public String encryptToBase64(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        ensureKey();

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("SSH 凭证加密失败", e);
        }
    }

    /**
     * 解密 Base64 字符串为明文。
     */
    public String decryptFromBase64(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            return null;
        }
        ensureKey();

        try {
            byte[] all = Base64.getDecoder().decode(encryptedBase64);
            if (all.length < IV_LENGTH + 1) {
                throw new IllegalArgumentException("encrypted payload too short");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            System.arraycopy(all, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("SSH 凭证解密失败", e);
        }
    }

    private void ensureKey() {
        if (secretKey == null) {
            throw new IllegalStateException("缺少配置 monitor.crypto.ssh-secret-key，无法加解密 SSH 凭证");
        }
    }
}

