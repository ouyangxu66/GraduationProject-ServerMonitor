-- 给服务器表增加创建人字段
ALTER TABLE `server_info`
    ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT '创建人账号' AFTER `id`;

-- (可选) 初始化旧数据：把现有的服务器都归属给 admin，防止旧数据查不到
UPDATE `server_info` SET `create_by` = 'admin' WHERE `create_by` IS NULL;

-- 确保普通用户(role_id=2) 拥有完整的服务器管理权限
-- menu_id 对应: 2=新增, 3=修改, 4=删除

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 2); -- server:add
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 3); -- server:edit
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2, 4); -- server:delete

-- ----------------------------
-- 用户Token表 (用于双Token机制)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_token`;
CREATE TABLE `sys_user_token` (
                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                  `refresh_token` varchar(512) NOT NULL COMMENT '刷新令牌',
                                  `expire_time` datetime NOT NULL COMMENT '过期时间',
                                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户Token表';