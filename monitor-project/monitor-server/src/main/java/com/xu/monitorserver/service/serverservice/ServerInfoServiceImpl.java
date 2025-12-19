package com.xu.monitorserver.service.serverservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.monitorcommon.constant.AppConstants;
import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.mapper.ServerInfoMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ServerInfoServiceImpl extends ServiceImpl<ServerInfoMapper, ServerInfo> implements IServerInfoService {

    private final StringRedisTemplate redisTemplate;

    public ServerInfoServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
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

        int updated = this.baseMapper.updateById(serverInfo);
        if (updated <= 0) {
            throw new ServiceException(500, "保存失败");
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