# If you are upgrading to 2024.01.1 from a different version, execute the following SQL statement
# Если вы обновляетесь до версии 2024.01.1 с другой версии, выполните следующую инструкцию SQL
# 如果您是通过其他版本升级到 2024.01.1, 请执行以下 SQL 语句

ALTER TABLE `metaforge_dashboard`
    ADD COLUMN `version` VARCHAR(100) COMMENT 'Dashboard version' DEFAULT 'V2';

UPDATE `metaforge_dashboard`
SET `version` = 'V1'
WHERE `version` IS NULL;

ALTER TABLE `audit_plugin`
    ADD COLUMN `query_mode` VARCHAR(100) DEFAULT 'ADHOC';

UPDATE `audit_plugin`
SET `query_mode` = 'ADHOC'
WHERE `query_mode` IS NULL;

ALTER TABLE `metaforge_dataset`
    ADD COLUMN `code` VARCHAR(100) default (uuid());

ALTER TABLE `metaforge_dataset_column`
    ADD COLUMN `column_mode` VARCHAR(100) default 'DIMENSION';

UPDATE `menus`
SET `is_new` = false
WHERE `is_new` = true;

CREATE TABLE `metaforge_report_dataset_relation`
(
    `report_id`  BIGINT,
    `dataset_id` BIGINT
);

UPDATE `template_sql`
SET `plugin` = 'MySQL,MatrixOne'
WHERE `plugin` = 'MySQL';

CREATE TABLE `metaforge_scheduled_history_source_relation`
(
    `scheduled_history_id` BIGINT,
    `source_id`            BIGINT
);

RENAME TABLE `source` TO `metaforge_source`;

ALTER TABLE `metaforge_dataset`
    CHANGE `sync_value` `expression` VARCHAR(100) NULL;

ALTER TABLE `metaforge_dataset`
    ADD COLUMN `scheduler` VARCHAR(100) NULL,
    ADD COLUMN `executor`  VARCHAR(100) DEFAULT 'Default';

ALTER TABLE `metaforge_dataset_column`
    ADD COLUMN `is_partition_key` BOOLEAN      DEFAULT FALSE,
    ADD COLUMN `alias_name`       VARCHAR(200) DEFAULT NULL;

CREATE TABLE `metaforge_dataset_history`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(255),
    `active`      BOOLEAN DEFAULT true,
    `create_time` DATETIME,
    `update_time` DATETIME,
    `query`       VARCHAR(255),
    `message`     TEXT,
    `elapsed`     BIGINT,
    `count`       INT,
    `query_mode`  VARCHAR(255),
    `state`       VARCHAR(255)
);

CREATE TABLE `metaforge_dataset_history_relation`
(
    `dataset_history_id` BIGINT,
    `dataset_id`         BIGINT
);

INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`, `create_time`, `update_time`, `is_system`)
VALUES ('SYSTEM_FOR_GET_ALL_DATABASES', 'SELECT
    catalog_name AS "CATALOG_NODE",
    schema_name AS "SCHEMA_NODE",
    NULL AS "CHARSET_NODE"
FROM
    information_schema.schemata;', 'Find a list of databases that already exist in all services
查找所有服务中已经存在的数据库列表', 'PostgreSQL',
        '[{"column":"database","type":"String","expression":"${database:String}"}]', '2024-01-18 11:21:34',
        '2024-01-18 11:21:34', 1);

INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`, `create_time`, `update_time`, `is_system`)
VALUES ('SYSTEM_FOR_GET_ALL_TABLES', 'SELECT
    table_catalog AS "CATALOG_NODE",
    table_schema AS "SCHEMA_NODE",
    table_name AS "TABLE_NODE",
    CASE table_type
        WHEN \'BASE TABLE\' THEN \'TABLE\'
        WHEN \'VIEW\' THEN \'VIEW\'
        WHEN \'MATERIALIZED VIEW\' THEN \'MATERIALIZED VIEW\'
        -- Add additional cases if needed
    END AS "TYPE_NODE",
    table_name AS "ENGINE_NODE", -- Note: PostgreSQL does not have ENGINE information
    NULL AS "FORMAT_NODE", -- Note: PostgreSQL does not have ROW_FORMAT information
    NULL AS "ROWS_NODE", -- Note: PostgreSQL does not have TABLE_ROWS information
    NULL AS "CREATE_TIME_NODE", -- Note: PostgreSQL does not have CREATE_TIME information
    NULL AS "UPDATE_TIME_NODE", -- Note: PostgreSQL does not have UPDATE_TIME information
    NULL AS "COLLATION_NODE",
    NULL AS "COMMENT_NODE",
    NULL AS "AVG_ROW_NODE", -- Note: PostgreSQL does not have AVG_ROW_LENGTH information
    NULL AS "DATA_NODE",
    NULL AS "INDEX_NODE", -- Note: PostgreSQL does not have INDEX_LENGTH information
    NULL AS "AUTO_INCREMENT_NODE" -- Note: PostgreSQL does not have AUTO_INCREMENT information
FROM
    information_schema.tables;', 'Queries the list of tables in the library according to the specified data name
根据指定数据名查询该库下数据表列表', 'PostgreSQL', '[]', '2024-01-18 11:32:48', '2024-01-18 11:32:48', 1);

INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`, `create_time`, `update_time`, `is_system`)
VALUES ('SYSTEM_FOR_GET_ALL_COLUMNS', 'SELECT
    table_schema AS "CATALOG_NODE",
    table_name AS "SCHEMA_NODE",
    column_name AS "COLUMN_NODE",
    column_default AS "DEFAULT_NODE",
    ordinal_position AS "POSITION_NODE",
    CASE is_nullable
        WHEN \'YES\' THEN TRUE
        WHEN \'NO\' THEN FALSE
    END AS "NULLABLE_NODE",
    data_type AS "DATA_TYPE_NODE",
    data_type AS "COLUMN_TYPE_NODE",
    NULL AS "EXTRA_NODE",
    character_maximum_length AS "MAXIMUM_LENGTH_NODE",
    character_octet_length AS "OCTET_LENGTH_NODE",
    numeric_precision AS "PRECISION_NODE",
    numeric_scale AS "SCALE_NODE",
    character_set_name AS "CHARACTER_NODE",
    collation_name AS "COLLATION_NODE",
    CASE is_identity
        WHEN \'YES\' THEN TRUE
        WHEN \'NO\' THEN FALSE
    END AS "KEY_NODE",
    NULL AS "PRIVILEGES_NODE",
    NULL AS "COMMENT_NODE"
FROM
    information_schema.columns;', 'According to the specified database, the data table gets all the data columns under the current condition
根据指定数据库，数据表获取当前条件下所有数据列', 'PostgreSQL', '[]', '2024-01-18 13:15:06', '2024-01-18 13:15:06', 1);

ALTER TABLE `metaforge_scheduled_history`
    ADD COLUMN `message` TEXT NULL;
