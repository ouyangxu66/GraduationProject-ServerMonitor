package com.xu.monitorclient.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Agent身份标识管理类
 * 负责为监控客户端生成和维护唯一身份标识(UUID)，确保每次启动时都能识别同一实例
 */
@Component
public class AgentIdentity {
    private static final Logger logger = LoggerFactory.getLogger(AgentIdentity.class);
    // 存储Agent ID的文件名
    private static final String ID_FILE = "agent.id";
    // Agent的唯一标识符
    private final String agentId;

    /**
     * 构造函数，在Bean初始化时调用loadOrCreateIdentity方法加载或创建身份标识
     */
    public AgentIdentity() {
        this.agentId = loadOrCreateIdentity();
    }

    /**
     * 获取Agent的唯一标识符
     * @return 返回Agent ID字符串
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * 加载或创建 Agent ID
     * 如果存在agent.id文件则从文件中读取ID，否则创建新的UUID并保存到文件中
     * @return 返回加载或新创建的Agent ID
     */
    private String loadOrCreateIdentity() {
        File file = new File(ID_FILE);
        try {
            if (file.exists()) {
                // 读取旧 ID
                String id = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
                logger.info("📦 加载现有 Agent Identity: {}", id);
                return id;
            } else {
                // 生成新 ID
                String id = UUID.randomUUID().toString();
                Files.writeString(file.toPath(), id, StandardCharsets.UTF_8);
                logger.info("✨ 生成新 Agent Identity: {}", id);
                return id;
            }
        } catch (IOException e) {
            throw new RuntimeException("无法读写 agent.id 文件，Agent 启动失败", e);
        }
    }
}