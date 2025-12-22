package com.xu.monitorserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


import java.time.LocalDateTime;


@TableName("server_info") // 对应数据库表名
public class ServerInfo {

    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    // 服务器在线状态 (非数据库字段)
    @TableField(exist = false)
    private Boolean isOnline;

    private String name;     // 别名
    private String ip;       // IP
    private Integer port;    // 端口
    private String username; // 用户名

    /**
     * SSH 密码。
     *
     * <p>历史原因：该字段目前仍是明文/或弱保护形式（取决于你之前的实现）。
     * 本次改造后建议在保存时做“应用层加密”后落库（见 {@code SshSecretCryptoService}）。</p>
     */
    private String password;

    /**
     * SSH 私钥（加密后存储）。
     *
     * <p>存储内容：Base64(AES-GCM(12bytes IV + ciphertext))。</p>
     * <p>注意：该字段不应直接返回给前端；前端通过 ticket 触发服务端侧连接。</p>
     */
    private String privateKeyEnc;

    /**
     * SSH 私钥口令（加密后存储，可为空）。
     */
    private String keyPassphraseEnc;

    /**
     * 私钥指纹（用于前端展示/审计，不含敏感信息）。
     */
    private String privateKeyFingerprint;

    /**
     * 是否已配置私钥（冗余字段，便于列表快速展示）。
     */
    private Integer hasPrivateKey;

    private String createBy; // 创建人
    private String agentId; // 唯一标识ID

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean online) {
        isOnline = online;
    }

    public String getPrivateKeyEnc() {
        return privateKeyEnc;
    }

    public void setPrivateKeyEnc(String privateKeyEnc) {
        this.privateKeyEnc = privateKeyEnc;
    }

    public String getKeyPassphraseEnc() {
        return keyPassphraseEnc;
    }

    public void setKeyPassphraseEnc(String keyPassphraseEnc) {
        this.keyPassphraseEnc = keyPassphraseEnc;
    }

    public String getPrivateKeyFingerprint() {
        return privateKeyFingerprint;
    }

    public void setPrivateKeyFingerprint(String privateKeyFingerprint) {
        this.privateKeyFingerprint = privateKeyFingerprint;
    }

    public Integer getHasPrivateKey() {
        return hasPrivateKey;
    }

    public void setHasPrivateKey(Integer hasPrivateKey) {
        this.hasPrivateKey = hasPrivateKey;
    }

    /**
     * 便捷方法：是否配置了私钥。
     *
     * <p>注意：这里不要加 {@code @TableField} 注解。
     * MyBatis-Plus 的 {@code @TableField(exist = false)} 只适用于字段，不适用于方法。
     *
     * <p>IDE 可能提示部分 getter/setter “未使用”，但它们会被：
     * <ul>
     *   <li>MyBatis-Plus 反射映射</li>
     *   <li>Jackson 序列化/反序列化</li>
     * </ul>
     * 这些运行时机制调用，因此请保留。</p>
     */
    public boolean hasPrivateKeyConfigured() {
        return hasPrivateKey != null && hasPrivateKey == 1;
    }
}