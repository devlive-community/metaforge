USE
    `metaforge`;

ALTER TABLE `template_sql`
    CHANGE COLUMN `system` `is_system` BOOLEAN;

CREATE TABLE `metaforge_metadata_database`
(
    id          INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(255),
    active      TINYINT(1) DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    description VARCHAR(1000),
    catalog     VARCHAR(255)
);

CREATE TABLE `metaforge_metadata_database_source_relation`
(
    source_id   INT,
    database_id INT
);

create table `metaforge_metadata_table`
(
    id             int auto_increment primary key,
    name           varchar(255)         null,
    active         tinyint(1) default 1 null,
    create_time    datetime             null,
    update_time    datetime             null,
    type           text                 null,
    engine         varchar(255)         null,
    format         varchar(255)         null,
    in_rows        varchar(255)         null,
    in_create_time varchar(255)         null,
    in_update_time varchar(255)         null,
    collation      varchar(255)         null,
    comment        varchar(1000)        null,
    description    varchar(1000)        null,
    avg_row_length varchar(100)         null,
    data_length    varchar(100)         null,
    index_length   varchar(100)         null,
    auto_increment varchar(100)         null
);

CREATE TABLE `metaforge_metadata_table_database_relation`
(
    table_id    INT,
    database_id INT
);

create table `metaforge_metadata_column`
(
    id             bigint auto_increment primary key,
    name           varchar(255)         null,
    active         tinyint(1) default 1 null,
    create_time    datetime             null,
    update_time    datetime             null,
    description    varchar(1000)        null,
    type           text                 null,
    comment        varchar(1000)        null,
    default_value  varchar(500)         null,
    position       varchar(100)         null,
    is_nullable    varchar(100)         null,
    maximum_length varchar(100)         null,
    collation      varchar(100)         null,
    is_key         varchar(100)         null,
    privileges     varchar(1000)        null,
    data_type      text                 null,
    extra          varchar(250)         null
);

CREATE TABLE `metaforge_metadata_column_table_relation`
(
    table_id  INT,
    column_id INT
);

CREATE TABLE `metaforge_scheduled_history`
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(255),
    active      BOOLEAN DEFAULT TRUE,
    create_time DATETIME,
    update_time DATETIME,
    info        TEXT,
    state       VARCHAR(100)
);

CREATE TABLE `metaforge_scheduled_history_relation`
(
    scheduled_id INT,
    history_id   INT
);

-- MySQL
INSERT INTO `template_sql` (name, content, description, plugin, configure, create_time, update_time, is_system)
VALUES ('SYSTEM_FOR_GET_ALL_DATABASES', 'SELECT
    `CATALOG_NAME` AS "CATALOG_NODE",
    `SCHEMA_NAME` AS "SCHEMA_NODE",
    `DEFAULT_CHARACTER_SET_NAME` AS "CHARSET_NODE"
FROM
    INFORMATION_SCHEMA.SCHEMATA', 'Find a list of databases that already exist in all services
查找所有服务中已经存在的数据库列表', 'MySQL', '[]', '2023-09-15 16:47:13', '2023-09-15 17:02:37', 1);
INSERT INTO `template_sql` (name, content, description, plugin, configure, create_time, update_time, is_system)
VALUES ('SYSTEM_FOR_GET_ALL_TABLES', 'SELECT
    `TABLE_CATALOG` AS "CATALOG_NODE",
    `TABLE_SCHEMA` AS "SCHEMA_NODE",
    `TABLE_NAME` AS "TABLE_NODE",
    CASE `TABLE_TYPE`
        WHEN \'BASE TABLE\' THEN \'TABLE\'
        WHEN \'VIEW\' THEN \'VIEW\'
        WHEN \'SYSTEM VIEW\' THEN \'SYSTEM\'
    END AS "TYPE_NODE",
    `ENGINE` AS "ENGINE_NODE",
    `ROW_FORMAT` AS "FORMAT_NODE",
    `TABLE_ROWS` AS "ROWS_NODE",
    `CREATE_TIME` AS "CREATE_TIME_NODE",
    `UPDATE_TIME` AS "UPDATE_TIME_NODE",
    `TABLE_COLLATION` AS "COLLATION_NODE",
    `TABLE_COMMENT` AS "COMMENT_NODE",
    `AVG_ROW_LENGTH` AS "AVG_ROW_NODE",
    `DATA_LENGTH` AS "DATA_NODE",
    `INDEX_LENGTH` AS "INDEX_NODE",
    `AUTO_INCREMENT` AS "AUTO_INCREMENT_NODE"
FROM
    information_schema.TABLES
-- WHERE
--    TABLE_SCHEMA = \'${database:String}\'', 'Queries the list of tables in the library according to the specified data name
根据指定数据名查询该库下数据表列表', 'MySQL', '[{"column":"database","type":"String","expression":"${database:String}"}]', '2023-09-18 20:10:53', '2023-10-10 10:37:14', 1);
INSERT INTO `template_sql` (name, content, description, plugin, configure, create_time, update_time, is_system)
VALUES ('SYSTEM_FOR_GET_ALL_COLUMNS', 'SELECT
    `TABLE_SCHEMA` AS "CATALOG_NODE",
    `TABLE_NAME` AS "SCHEMA_NODE",
    `COLUMN_NAME` AS "COLUMN_NODE",
    `COLUMN_DEFAULT` AS "DEFAULT_NODE",
    `ORDINAL_POSITION` AS "POSITION_NODE",
    CASE `IS_NULLABLE`
        WHEN \'YES\' THEN TRUE
        WHEN \'NO\' THEN FALSE
    END AS "NULLABLE_NODE",
    `DATA_TYPE` AS "DATA_TYPE_NODE",
    `COLUMN_TYPE` AS "COLUMN_TYPE_NODE",
    `EXTRA` AS "EXTRA_NODE",
    `CHARACTER_MAXIMUM_LENGTH` AS "MAXIMUM_LENGTH_NODE",
    `CHARACTER_OCTET_LENGTH` AS "OCTET_LENGTH_NODE",
    `NUMERIC_PRECISION` AS "PRECISION_NODE",
    `NUMERIC_SCALE` AS "SCALE_NODE",
    `NUMERIC_SCALE` AS "SCALE_NODE",
    `CHARACTER_SET_NAME` AS "CHARACTER_NODE",
    `COLLATION_NAME` AS "COLLATION_NODE",
    `COLUMN_KEY` AS "KEY_NODE",
    `PRIVILEGES` AS "PRIVILEGES_NODE",
    `COLUMN_COMMENT` AS "COMMENT_NODE"
FROM
    information_schema.COLUMNS
-- WHERE
--     TABLE_SCHEMA = \'${database:String}\'
-- AND TABLE_NAME = \'${table:String}\';', 'According to the specified database, the data table gets all the data columns under the current condition
根据指定数据库，数据表获取当前条件下所有数据列', 'MySQL',
        '[{"column":"database","type":"String","expression":"${database:String}"},{"column":"table","type":"String","expression":"${table:String}"}]', '2023-09-18 23:40:26',
        '2023-10-10 11:52:49', 1);

-- Clickhouse
INSERT INTO `template_sql` (name, content, description, plugin, configure, create_time, update_time, is_system)
VALUES ('SYSTEM_FOR_GET_ALL_DATABASES', 'SELECT
    \'public\' AS "CATALOG_NODE",
    `name` AS "SCHEMA_NODE",
    NULL AS "CHARSET_NODE"
FROM
    system.databases', 'Find a list of databases that already exist in all services
查找所有服务中已经存在的数据库列表', 'ClickHouse', '[]', '2023-10-09 20:13:16', '2023-10-10 10:33:11', 1);
INSERT INTO `template_sql` (name, content, description, plugin, configure, create_time, update_time, is_system)
VALUES ('SYSTEM_FOR_GET_ALL_TABLES', 'SELECT
    \'public\' AS "CATALOG_NODE",
    `database` AS "SCHEMA_NODE",
    `name` AS "TABLE_NODE",
    multiIf(`engine` = \'MaterializedView\', \'VIEW\', \'TABLE\') AS "TYPE_NODE",
    `engine` AS "ENGINE_NODE",
    `engine` AS "FORMAT_NODE",
    if(isNotNull(`total_rows`), `total_rows`, 0) AS "ROWS_NODE",
    NULL AS "CREATE_TIME_NODE",
    `metadata_modification_time` AS "UPDATE_TIME_NODE",
    \'utf8\' AS "COLLATION_NODE",
    \'\' AS "COMMENT_NODE",
    if(isNaN(round(`total_bytes` / `total_rows`, 2)), 0, round(`total_bytes` / `total_rows`, 2)) AS "AVG_ROW_NODE",
    `total_bytes` AS "DATA_NODE",
    NULL AS "INDEX_NODE",
    NULL AS "AUTO_INCREMENT_NODE"
FROM
    system.tables
-- WHERE
--    `database` = \'${database:String}\'', 'Queries the list of tables in the library according to the specified data name
根据指定数据名查询该库下数据表列表', 'ClickHouse', '[{"column":"database","type":"String","expression":"${database:String}"}]', '2023-10-09 21:01:44', '2023-10-10 12:36:17', 1);
INSERT INTO `template_sql` (name, content, description, plugin, configure, create_time, update_time, is_system)
VALUES ('SYSTEM_FOR_GET_ALL_COLUMNS', 'SELECT
    `database` AS "CATALOG_NODE",
    `table` AS "SCHEMA_NODE",
    `name` AS "COLUMN_NODE",
    `default_expression` AS "DEFAULT_NODE",
    `position` AS "POSITION_NODE",
    if(position(`type`, \'Nullable\') > 0, 1, 0) AS "NULLABLE_NODE",
    `type` AS "DATA_TYPE_NODE",
    `type` AS "COLUMN_TYPE_NODE",
    \'\' AS "EXTRA_NODE",
    0 AS "MAXIMUM_LENGTH_NODE",
    0 AS "OCTET_LENGTH_NODE",
    \'\' AS "PRECISION_NODE",
    \'\' AS "SCALE_NODE",
    \'utf8\' AS "CHARACTER_NODE",
    \'utf8\' AS "COLLATION_NODE",
    if(`is_in_primary_key` == 1, \'PRI\', NULL) AS "KEY_NODE",
    \'\' AS "PRIVILEGES_NODE",
    `comment` AS "COMMENT_NODE"
FROM
    system.columns
-- WHERE
--     `database` = \'${database:String}\'
-- AND `table` = \'${table:String}\';', 'According to the specified database, the data table gets all the data columns under the current condition
根据指定数据库，数据表获取当前条件下所有数据列', 'ClickHouse',
        '[{"column":"database","type":"String","expression":"${database:String}"},{"column":"table","type":"String","expression":"${table:String}"}]', '2023-10-09 21:39:02',
        '2023-10-10 12:37:38', 1);

