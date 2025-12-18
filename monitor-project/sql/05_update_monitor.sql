-- 修改服务器表
ALTER TABLE `server_info`
ADD COLUMN `agent_id` varchar(64) DEFAULT NULL COMMENT '探针唯一ID(UUID)' AFTER `id`,
ADD INDEX `idx_agent_id` (`agent_id`); -- 加索引方便查询

-- 增加一个状态字段 (可选，或者直接查 Redis)
-- ALTER TABLE `sys_server` ADD COLUMN `status` int DEFAULT 0 COMMENT '0离线 1在线';