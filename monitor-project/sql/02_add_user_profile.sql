/*
 * 数据库更新脚本 v1.1
 * 目标: 丰富用户个人资料字段
 */

-- 1. 修改 sys_user 表，新增字段
ALTER TABLE `sys_user`
    ADD COLUMN `nickname` varchar(64) DEFAULT NULL COMMENT '用户昵称' AFTER `username`,
    ADD COLUMN `avatar` varchar(255) DEFAULT '' COMMENT '头像地址(URL)' AFTER `nickname`,
    ADD COLUMN `email` varchar(128) DEFAULT NULL COMMENT '电子邮箱' AFTER `avatar`,
    ADD COLUMN `bio` varchar(500) DEFAULT NULL COMMENT '个人简介' AFTER `email`,
    ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;

-- 2. 初始化现有账号的数据 (防止字段为NULL影响显示)
UPDATE `sys_user`
SET `nickname` = '超级管理员',
    `avatar` = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    `bio` = '系统默认管理员账号'
WHERE `username` = 'admin';