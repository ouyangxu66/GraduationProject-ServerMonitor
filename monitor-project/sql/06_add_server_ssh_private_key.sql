-- WebSSH 双认证升级：为 server_info 增加“私钥(加密存储)+指纹+是否配置私钥+私钥口令(加密存储)”相关字段
-- 说明：
-- 1) private_key_enc / key_passphrase_enc 建议存储 Base64(AES-GCM(ciphertext))，避免明文落库。
-- 2) private_key_fingerprint 仅用于前端展示与审计，不包含敏感明文。
-- 3) has_private_key 为冗余字段，便于列表页快速显示“已配置/未配置”。

ALTER TABLE `server_info`
    ADD COLUMN `private_key_enc` MEDIUMTEXT DEFAULT NULL COMMENT 'SSH 私钥(加密后存储，Base64)' AFTER `password`,
    ADD COLUMN `key_passphrase_enc` TEXT DEFAULT NULL COMMENT 'SSH 私钥口令(加密后存储，Base64，可为空)' AFTER `private_key_enc`,
    ADD COLUMN `private_key_fingerprint` VARCHAR(128) DEFAULT NULL COMMENT '私钥指纹(用于展示/审计，不含明文)' AFTER `key_passphrase_enc`,
    ADD COLUMN `has_private_key` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已配置私钥(冗余字段)' AFTER `private_key_fingerprint`;

CREATE INDEX `idx_server_has_private_key` ON `server_info`(`has_private_key`);

