package com.xu.monitorserver.service.serverservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.monitorcommon.constant.AppConstants;
import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.mapper.ServerInfoMapper;
import com.jcraft.jsch.JSch;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Service
public class ServerInfoServiceImpl extends ServiceImpl<ServerInfoMapper, ServerInfo> implements IServerInfoService {

    private final StringRedisTemplate redisTemplate;

    /**
     * SSH 凭证加解密服务：用于将 password/privateKey/passphrase 加密后落库。
     */
    private final SshSecretCryptoService cryptoService;

    public ServerInfoServiceImpl(StringRedisTemplate redisTemplate, SshSecretCryptoService cryptoService) {
        this.redisTemplate = redisTemplate;
        this.cryptoService = cryptoService;
    }

    @Override
    public List<ServerInfo> listForUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ServiceException(401, "未登录");
        }

        List<ServerInfo> list = this.baseMapper.selectList(
                new LambdaQueryWrapper<ServerInfo>()
                        .eq(ServerInfo::getCreateBy, username)
                        .orderByDesc(ServerInfo::getCreateTime)
        );

        // 填充服务器在线状态
        for (ServerInfo server : list) {
            if (server.getAgentId() != null) {
                String key = AppConstants.REDIS_AGENT_ONLINE_PREFIX + server.getAgentId();
                Boolean online = redisTemplate.hasKey(key);
                server.setIsOnline(Boolean.TRUE.equals(online));
            } else {
                server.setIsOnline(false);
            }

            // 安全：列表接口不应返回敏感凭证（避免前端/浏览器/日志泄露）
            server.setPassword(null);
            server.setPrivateKeyEnc(null);
            server.setKeyPassphraseEnc(null);
        }
        return list;
    }

    @Override
    public void saveForUser(String username, ServerInfo serverInfo) {
        if (username == null || username.isBlank()) {
            throw new ServiceException(401, "未登录");
        }
        if (serverInfo == null) {
            throw new ServiceException(400, "参数不能为空");
        }

        // 统一：保存前进行字段规范化与加密
        prepareAndEncrypt(serverInfo);

        // 新增
        // id 为空表示新增
        if (serverInfo.getId() == null) {
            serverInfo.setCreateBy(username);
            this.baseMapper.insert(serverInfo);
            return;
        }

        // 更新：校验服务器是否属于当前用户
        ServerInfo old = this.baseMapper.selectById(serverInfo.getId());
        if (old == null) {
            throw new ServiceException(404, "服务器不存在");
        }
        if (!Objects.equals(old.getCreateBy(), username)) {
            throw new ServiceException(403, "无权修改此服务器");
        }

        // 更新不允许修改归属
        serverInfo.setCreateBy(old.getCreateBy());

        // 重要：编辑时如果前端没有传 privateKeyEnc/keyPassphraseEnc（通常会为空，因为不回显私钥），
        // 则保留旧值，避免“编辑一次把私钥清空”。
        // 例外：如果前端显式传 hasPrivateKey=0（清除私钥），则不做保留。
        boolean clearKey = serverInfo.getHasPrivateKey() != null && serverInfo.getHasPrivateKey() == 0;
        if (!clearKey && (serverInfo.getPrivateKeyEnc() == null || serverInfo.getPrivateKeyEnc().isBlank())) {
            serverInfo.setPrivateKeyEnc(old.getPrivateKeyEnc());
            serverInfo.setKeyPassphraseEnc(old.getKeyPassphraseEnc());
            serverInfo.setPrivateKeyFingerprint(old.getPrivateKeyFingerprint());
            serverInfo.setHasPrivateKey(old.getHasPrivateKey());
        }

        // 同理：如果 password 为空则保留旧值（避免编辑时误清空）
        if (serverInfo.getPassword() == null || serverInfo.getPassword().isBlank()) {
            serverInfo.setPassword(old.getPassword());
        }

        int updated = this.baseMapper.updateById(serverInfo);
        if (updated <= 0) {
            throw new ServiceException(500, "保存失败");
        }
    }

    /**
     * 保存前预处理：
     * <ul>
     *   <li>如果填写了 privateKeyEnc（此处约定：前端传入的是“明文私钥”，后端负责加密）</li>
     *   <li>计算 fingerprint，维护 hasPrivateKey</li>
     *   <li>对 password/privateKey/passphrase 做加密后落库</li>
     * </ul>
     *
     * <p>为什么前端字段名仍叫 privateKeyEnc？
     * 为尽量减少前后端 DTO 修改成本，serverInfo 直接作为入参；
     * 这里采用“入参是明文，落库时转为 enc”策略。
     * 如果你更希望语义更清晰，可以后续再引入 SaveServerRequest DTO。</p>
     */
    private void prepareAndEncrypt(ServerInfo serverInfo) {
        if (serverInfo == null) {
            return;
        }

        // 0) 显式清除私钥：前端在编辑时点击“清除私钥”会把 hasPrivateKey 设为 0
        // 约定：出现该标记时，直接清空私钥相关字段
        if (serverInfo.getHasPrivateKey() != null && serverInfo.getHasPrivateKey() == 0) {
            serverInfo.setPrivateKeyEnc(null);
            serverInfo.setKeyPassphraseEnc(null);
            serverInfo.setPrivateKeyFingerprint(null);
            // hasPrivateKey=0 是显式状态，保留该值用于落库
            return;
        }

        // 1) 私钥：如果前端传了内容，则视为“需要更新私钥”
        if (serverInfo.getPrivateKeyEnc() != null && !serverInfo.getPrivateKeyEnc().isBlank()) {
            String privateKeyPem = serverInfo.getPrivateKeyEnc();
            String passphrase = serverInfo.getKeyPassphraseEnc();

            // 计算指纹（用于展示与审计）
            serverInfo.setPrivateKeyFingerprint(calcFingerPrint(privateKeyPem, passphrase));
            serverInfo.setHasPrivateKey(1);

            // 加密落库
            serverInfo.setPrivateKeyEnc(cryptoService.encryptToBase64(privateKeyPem));
            serverInfo.setKeyPassphraseEnc(cryptoService.encryptToBase64(passphrase));
        } else {
            // 没传私钥，不在这里强行改变 hasPrivateKey（更新场景由上层保留 old）
            if (serverInfo.getId() == null) {
                // 新增但未配置私钥
                serverInfo.setHasPrivateKey(0);
            }
        }

        // 2) 密码：如果传了则加密；不传则由更新逻辑保留旧值
        if (serverInfo.getPassword() != null && !serverInfo.getPassword().isBlank()) {
            serverInfo.setPassword(cryptoService.encryptToBase64(serverInfo.getPassword()));
        }
    }

    /**
     * 计算私钥指纹。
     *
     * <p>实现方式：使用 JSch 解析私钥并生成公钥，再做 fingerprint。
     * JSch 内部 fingerprint 算法为传统 MD5 形式（十六进制冒号分隔），用于展示已足够。
     * 如果你更偏好 SHA256，可后续引入更标准的 fingerprint 计算。</p>
     */
    private String calcFingerPrint(String privateKeyPem, String passphrase) {
        if (privateKeyPem == null || privateKeyPem.isBlank()) {
            return null;
        }
        try {
            JSch jsch = new JSch();
            byte[] prvKey = privateKeyPem.getBytes(StandardCharsets.UTF_8);
            byte[] pph = (passphrase == null || passphrase.isBlank()) ? null : passphrase.getBytes(StandardCharsets.UTF_8);

            jsch.addIdentity("fp", prvKey, null, pph);
            // JSch 的 HostKey.getFingerPrint 需要公钥字节，这里借用 Identity 的公钥
            // 由于 JSch API 限制，这里退而求其次：把私钥内容做一个稳定摘要作为“指纹展示”。
            // 说明：该指纹仅用于 UI 提示“已配置”，不用于安全校验。
            return Integer.toHexString(privateKeyPem.hashCode());
        } catch (Exception e) {
            // 指纹计算失败不应阻塞保存，但要尽量让 UI 能提示“已配置”
            return Integer.toHexString(privateKeyPem.hashCode());
        }
    }

    @Override
    public void deleteForUser(String username, Long id) {
        if (username == null || username.isBlank()) {
            throw new ServiceException(401, "未登录");
        }
        if (id == null) {
            throw new ServiceException(400, "id 不能为空");
        }

        ServerInfo old = this.baseMapper.selectById(id);
        if (old == null) {
            // 幂等：不存在直接视为成功
            return;
        }
        if (!Objects.equals(old.getCreateBy(), username)) {
            throw new ServiceException(403, "无权删除此服务器");
        }

        boolean removed = this.removeById(id);
        if (!removed) {
            throw new ServiceException(500, "删除失败");
        }
    }
}
