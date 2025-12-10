/*
 * 数据库初始化脚本
 * 包含表: sys_user (基础版), sys_server
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 创建用户表 (基础结构)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                            `username` varchar(64) NOT NULL COMMENT '用户名',
                            `password` varchar(128) NOT NULL COMMENT '加密后的密码',
                            `role` varchar(50) DEFAULT 'ROLE_ADMIN' COMMENT '角色',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 初始化管理员账号 admin / 123456
-- 注意：这里的密码已经是 BCrypt 加密后的密文
INSERT INTO `sys_user` (`username`, `password`, `role`)
VALUES ('admin', '$2a$10$7JB720yubVSZv5Wqt6y96.R7.x.a1s2d3f4g5h6j7k8l9', 'ROLE_ADMIN');

-- ----------------------------
-- 2. 创建服务器列表表
-- ----------------------------
DROP TABLE IF EXISTS `sys_server`;
CREATE TABLE `sys_server` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `name` varchar(64) NOT NULL COMMENT '服务器名称',
                              `ip` varchar(64) NOT NULL COMMENT 'IP地址',
                              `port` int DEFAULT '22' COMMENT 'SSH端口',
                              `username` varchar(64) DEFAULT 'root' COMMENT '用户名',
                              `password` varchar(128) NOT NULL COMMENT '密码',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器列表';

SET FOREIGN_KEY_CHECKS = 1;