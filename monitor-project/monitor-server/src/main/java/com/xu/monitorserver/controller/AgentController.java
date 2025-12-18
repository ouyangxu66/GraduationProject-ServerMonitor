package com.xu.monitorserver.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.monitorcommon.constant.AppConstants;
import com.xu.monitorcommon.dto.AgentDTO;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.mapper.ServerInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    private final ServerInfoMapper serverMapper;
    private final StringRedisTemplate redisTemplate;
    public AgentController(ServerInfoMapper serverMapper,StringRedisTemplate redisTemplate){
        this.redisTemplate=redisTemplate;
        this.serverMapper=serverMapper;
    }

    @Value("${monitor.app-secret}")
    private String serverAppSecret;

    /**
     * Agent 启动注册
     * 逻辑：
     * 1. 记录 Agent 在线状态
     * 2. 检查 sys_server 表是否已存在该 agentId
     * 3. 如果存在 -> 更新 IP、HostName 等信息
     * 4. 如果不存在 -> (可选) 自动创建一条新记录，或者忽略等待管理员手动绑定
     */
    @PostMapping("/register")
    public R<Void> register(@RequestBody AgentDTO.Register dto,
                            @RequestHeader(value = AppConstants.MONITOR_APP_SECRET_HEADER, required = false) String secretHeader) {
        logger.info("收到 Agent 注册请求: {}", dto);

        //鉴权校验
        checkSecret(secretHeader);
        // 1. 标记在线 (TTL 60秒，心跳间隔30秒，容错率2倍)
        refreshOnlineStatus(dto.getAgentId());

        // 2. 查询数据库是否已有该 Agent
        ServerInfo existingServer = serverMapper.selectOne(
                new LambdaQueryWrapper<ServerInfo>().eq(ServerInfo::getAgentId, dto.getAgentId())
        );

        if (existingServer != null) {
            // 2.1 已存在：更新最新的 IP 和 HostName (应对 DHCP 变动)
            existingServer.setIp(dto.getIp());
            existingServer.setName(dto.getHostname()); // 可选：是否覆盖名称看业务需求
            existingServer.setUpdateTime(LocalDateTime.now());
            serverMapper.updateById(existingServer);
        } else {
            // 2.2 不存在：
            // 策略A：自动入库 (Auto Discovery) - 推荐
            ServerInfo newServer = new ServerInfo();
            newServer.setAgentId(dto.getAgentId());
            newServer.setName(dto.getHostname());
            newServer.setIp(dto.getIp());
            newServer.setPort(22); // 默认 SSH 端口
            newServer.setCreateBy("system"); // 标记为系统自动发现
            newServer.setCreateTime(LocalDateTime.now());
            serverMapper.insert(newServer);
            
            // 策略B：不入库，只在 Redis 记录一个 "待接入列表"，让管理员在前端手动点"准入"
            // (这里我们采用策略A，简单直接)
        }

        return R.ok();
    }

    /**
     * Agent 心跳
     * 逻辑：仅续期 Redis Key
     */
    @PostMapping("/heartbeat")
    public R<Void> heartbeat(@RequestBody AgentDTO.Heartbeat dto,
                             @RequestHeader(value = AppConstants.MONITOR_APP_SECRET_HEADER, required = false) String secretHeader) {
        logger.info("收到 Agent 心跳请求: {}", dto);
        //鉴权校验
        checkSecret(secretHeader);

        refreshOnlineStatus(dto.getAgentId());
        return R.ok();
    }

    /**
     * 辅助方法：刷新 Redis 在线状态
     * Key: agent:online:uuid
     * Value: 1
     * TTL: 60s
     */
    private void refreshOnlineStatus(String agentId) {
        String key = AppConstants.REDIS_AGENT_ONLINE_PREFIX + agentId;
        redisTemplate.opsForValue().set(key, "1", 60, TimeUnit.SECONDS);
    }

    /**
     * 鉴权逻辑:鉴别请求头是否携带密钥
     * @param secretHeader
     */
    private void checkSecret(String secretHeader){
        if (serverAppSecret == null || serverAppSecret.isEmpty()){
            return;
        }
        if (!serverAppSecret.equals(secretHeader)){
            throw new RuntimeException("非法访问: App Secret 错误或异常");
        }
    }
}