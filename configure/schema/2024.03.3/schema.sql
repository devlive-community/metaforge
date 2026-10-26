# If you are upgrading to 2024.03.3 from a different version, execute the following SQL statement
# Если вы обновляетесь до версии 2024.03.3 с другой версии, выполните следующую инструкцию SQL
# 如果您是通过其他版本升级到 2024.03.3, 请执行以下 SQL 语句

USE `metaforge`;

RENAME
TABLE `audit_plugin` TO `metaforge_source_query`;

ALTER TABLE `metaforge_source_query`
    CHANGE `end_time` `update_time` datetime;

ALTER TABLE `metaforge_source_query`
    ADD `name` VARCHAR(100),
    ADD COLUMN `active` BOOLEAN DEFAULT TRUE,
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_source_query`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_pipeline`
    ADD COLUMN `active` BOOLEAN DEFAULT TRUE,
    ADD COLUMN `code` VARCHAR(100);

ALTER TABLE `metaforge_pipeline`
    CHANGE `start_time` `create_time` DATETIME,
    CHANGE `end_time` `update_time` DATETIME;

UPDATE `metaforge_pipeline`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_scheduled`
    ADD COLUMN `code` varchar(100);

UPDATE `metaforge_scheduled`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

RENAME
TABLE `role` TO `metaforge_role`;

ALTER TABLE `metaforge_role`
    ADD COLUMN `active` BOOLEAN DEFAULT TRUE,
    ADD COLUMN `code` VARCHAR(100),
    ADD COLUMN `update_time` DATETIME;

UPDATE `metaforge_role`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

RENAME
TABLE `role_menu_relation` TO `metaforge_role_menu_relation`;

ALTER TABLE `metaforge_dashboard`
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_dashboard`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_source`
    ADD COLUMN `active` BOOLEAN DEFAULT TRUE;

UPDATE `metaforge_source`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

RENAME
TABLE `snippet` TO `metaforge_snippet`;

ALTER TABLE `metaforge_snippet`
    CHANGE `code` `context` TEXT;

ALTER TABLE `metaforge_snippet`
    ADD COLUMN `code` VARCHAR(100),
    ADD COLUMN `active` BOOLEAN DEFAULT TRUE;

UPDATE `metaforge_snippet`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_report`
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_report`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_metadata_database`
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_metadata_database`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_metadata_table`
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_metadata_table`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_metadata_column`
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_metadata_column`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_scheduled_history`
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_scheduled_history`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

RENAME
TABLE `menus` TO `metaforge_menu`;

UPDATE `metaforge_menu`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

RENAME
TABLE `pipeline_user_relation` TO `metaforge_pipeline_user_relation`;

RENAME
TABLE `user_roles` TO `metaforge_user_role_relation`;

RENAME
TABLE `functions` TO `metaforge_function`;

UPDATE `metaforge_function`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

RENAME
TABLE `template_sql` TO `metaforge_template`;

UPDATE `metaforge_template`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

RENAME
TABLE `user_log` TO `metaforge_user_log`;

ALTER TABLE `metaforge_function`
    ADD COLUMN `active` BOOLEAN DEFAULT TRUE,
    ADD COLUMN `code` VARCHAR(100);

ALTER TABLE `metaforge_template`
    ADD COLUMN `active` BOOLEAN DEFAULT TRUE,
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_dataset`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_dataset_column`
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_dataset_column`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_dataset_history`
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_dataset_history`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;

ALTER TABLE `metaforge_source_query`
    ADD COLUMN `home` VARCHAR(500);

ALTER TABLE `metaforge_report`
    ADD COLUMN `description` VARCHAR(2000);

ALTER TABLE `metaforge_dashboard`
    CHANGE `version` `description` VARCHAR(1000) DEFAULT NULL COMMENT 'Description';

ALTER TABLE `metaforge_chat`
    ADD COLUMN `code` VARCHAR(100);

ALTER TABLE `metaforge_message`
    ADD COLUMN `code` VARCHAR(100);
