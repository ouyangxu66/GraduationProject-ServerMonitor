-- 给服务器表增加创建人字段
ALTER TABLE `server_info`
    ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT '创建人账号' AFTER `id`;

-- (可选) 初始化旧数据：把现有的服务器都归属给 admin，防止旧数据查不到
UPDATE `server_info` SET `create_by` = 'admin' WHERE `create_by` IS NULL;