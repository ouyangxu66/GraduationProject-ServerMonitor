/*
 * 数据库更新脚本 v1.2
 * 目标: 实现逻辑删除与细粒度 RBAC 权限控制
 */

-- 1. 升级 sys_user 表：增加逻辑删除字段
ALTER TABLE `sys_user` ADD COLUMN `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0否 1是)';

-- 2. 新增 sys_role 表 (角色表)
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `role_name` varchar(64) NOT NULL COMMENT '角色名称',
                            `role_code` varchar(64) NOT NULL COMMENT '角色编码(ROLE_ADMIN)',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 新增 sys_menu 表 (权限/菜单表)
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `menu_name` varchar(64) NOT NULL COMMENT '菜单名称',
                            `perms` varchar(100) DEFAULT NULL COMMENT '权限标识(如 server:add)',
                            `type` char(1) DEFAULT 'B' COMMENT '类型(M目录 C菜单 B按钮)',
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限菜单表';

-- 4. 新增 sys_role_menu 表 (角色-权限关联表)
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
                                 `role_id` bigint NOT NULL,
                                 `menu_id` bigint NOT NULL,
                                 PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ==========================================
-- 初始化权限数据
-- ==========================================

-- 初始化角色
INSERT INTO `sys_role` (`role_name`, `role_code`) VALUES
                                                      ('超级管理员', 'ROLE_ADMIN'),
                                                      ('普通用户', 'ROLE_USER');

-- 初始化权限点 (服务器管理的增删改查)
INSERT INTO `sys_menu` (`id`, `menu_name`, `perms`, `type`) VALUES
                                                                (1, '服务器查询', 'server:list', 'B'),
                                                                (2, '服务器新增', 'server:add', 'B'),
                                                                (3, '服务器修改', 'server:edit', 'B'),
                                                                (4, '服务器删除', 'server:delete', 'B');

-- 给管理员(id=1)分配所有权限 (1,2,3,4)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
                                                       (1, 1), (1, 2), (1, 3), (1, 4);

-- 给普通用户(id=2)分配仅查询权限 (1)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (2, 1);