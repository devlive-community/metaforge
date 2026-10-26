# If you are upgrading to 1.18.0 from a different version, execute the following SQL statement
# Если вы обновляетесь до версии 1.18.0 с другой версии, выполните следующую инструкцию SQL
# 如果您是通过其他版本升级到 1.18.0, 请执行以下 SQL 语句

ALTER TABLE `metaforge_user`
    ADD COLUMN `avatar_configure` LONGTEXT COMMENT 'avatar configure';

UPDATE `metaforge_user`
SET `avatar_configure` = '{}'
WHERE `avatar_configure` IS NULL;

UPDATE `metaforge_user`
SET `editor_configure` = '{}'
WHERE `editor_configure` IS NULL;

DELETE
FROM `audit_plugin`
WHERE `plugin_id` = 9;

ALTER TABLE `metaforge_pipeline`
    ADD COLUMN `flow_configure` LONGTEXT COMMENT 'Flow configure';

CREATE TABLE `metaforge_report`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(255),
    `active`      BOOLEAN DEFAULT TRUE,
    `create_time` DATETIME,
    `update_time` DATETIME,
    `configure`   LONGTEXT,
    `type`        VARCHAR(255),
    `realtime`    BOOLEAN DEFAULT FALSE,
    `query`       LONGTEXT
);

CREATE TABLE `metaforge_report_user_relation`
(
    `report_id` BIGINT,
    `user_id`   BIGINT
);

INSERT INTO `menus` (`name`, `code`, `description`, `url`, `group_name`, `sorted`, `type`, `parent`, `active`, `i18n_key`, `icon`, `create_time`, `update_time`, `redirect`,
                     `is_new`)
VALUES ('全局 - 管理 - 报表', 'REPORT', '全局：所有用户都可以访问
位置：顶部管理一级子菜单', '/admin/report', null, 6, 'VIEW', 3, 1, 'common.report', 'md-analytics', '2023-12-18 13:37:35', null, 0, 1);
INSERT INTO `role_menu_relation` (`role_id`, `menu_id`)
VALUES ('2', '15');

INSERT INTO `menus` (`name`, `code`, `description`, `url`, `group_name`, `sorted`, `type`, `parent`, `active`, `i18n_key`, `icon`, `create_time`, `update_time`, `redirect`,
                     `is_new`)
VALUES ('全局 - 仪表盘', 'DASHBOARD', '全局路由：所有用户都可以访问', '/console/dashboard', null, 2, 'DASHBOARD', 0, 1, 'common.dashboard', 'md-analytics', '2023-12-19 10:26:21',
        null,
        0, 1);
INSERT INTO `role_menu_relation` (`role_id`, `menu_id`)
VALUES ('1', '16');
INSERT INTO `role_menu_relation` (`role_id`, `menu_id`)
VALUES ('2', '16');

INSERT INTO `menus` (`name`, `code`, `description`, `url`, `group_name`, `sorted`, `type`, `parent`, `active`, `i18n_key`, `icon`, `create_time`, `update_time`, `redirect`,
                     `is_new`)
VALUES ('全局 - 数据集', 'DATASET', '全局路由：所有用户都可以访问', '/admin/dataset', null, 3, 'VIEW', 0, 1,
        'common.dataset', 'md-contrast', '2023-12-21 11:32:33', null, 0, 1);
INSERT INTO `role_menu_relation` (`role_id`, `menu_id`)
VALUES ('1', '17');
INSERT INTO `role_menu_relation` (`role_id`, `menu_id`)
VALUES ('2', '17');

CREATE TABLE `metaforge_dashboard`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(255),
    `active`      BOOLEAN DEFAULT TRUE,
    `create_time` DATETIME,
    `update_time` DATETIME,
    `configure`   LONGTEXT
);

CREATE TABLE `metaforge_dashboard_user_relation`
(
    `dashboard_id` BIGINT,
    `user_id`      BIGINT
);

CREATE TABLE `metaforge_dashboard_report_relation`
(
    `dashboard_id` BIGINT,
    `report_id`    BIGINT
);

CREATE TABLE `metaforge_report_source_relation`
(
    `report_id` BIGINT,
    `source_id` BIGINT
);

UPDATE `metaforge_metadata_database`
SET `active` = 1
WHERE `active` IS NULL;

UPDATE `metaforge_metadata_table`
SET `active` = 1
WHERE `active` IS NULL;

UPDATE `metaforge_metadata_column`
SET `active` = 1
WHERE `active` IS NULL;

CREATE TABLE `metaforge_dataset`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(255),
    `active`      BOOLEAN DEFAULT TRUE,
    `create_time` DATETIME,
    `update_time` DATETIME,
    `description` TEXT,
    `query`       LONGTEXT,
    `sync_mode`   VARCHAR(100),
    `sync_value`  VARCHAR(100),
    `table_name`  VARCHAR(255)
);

CREATE TABLE `metaforge_dataset_user_relation`
(
    `dataset_id` BIGINT,
    `user_id`    BIGINT
);

CREATE TABLE `metaforge_dataset_source_relation`
(
    `dataset_id` BIGINT,
    `source_id`  BIGINT
);

CREATE TABLE `metaforge_dataset_column`
(
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`            VARCHAR(255),
    `active`          BOOLEAN DEFAULT TRUE,
    `create_time`     DATETIME,
    `update_time`     DATETIME,
    `description`     TEXT,
    `type`            VARCHAR(100),
    `comment`         VARCHAR(255),
    `original`        VARCHAR(255),
    `default_value`   VARCHAR(255),
    `position`        INT,
    `is_nullable`     BOOLEAN DEFAULT FALSE,
    `length`          INT,
    `state`           VARCHAR(100),
    `message`         LONGTEXT,
    `is_order_by_key` BOOLEAN DEFAULT FALSE
);

CREATE TABLE `metaforge_dataset_column_relation`
(
    `dataset_id` BIGINT,
    `column_id`  BIGINT
);
