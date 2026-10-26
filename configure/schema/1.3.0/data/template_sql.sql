USE `metaforge`;

INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`)
VALUES ('getAllDatabase', 'SHOW DATABASES', 'Gets a list of all databases', 'ClickHouse,MySQL', '{}'),
       ('getAllTablesFromDatabase', 'SHOW TABLES FROM ${database:String}', 'Get the data table from the database', 'ClickHouse,MySQL',
        '[{\"column\":\"database\",\"type\":\"String\",\"expression\":\"${database:String}\"}]'),
       ('getAllDatabaseAndTable', 'SELECT database as tableSchema, name as tableName\nFROM system.tables\nWHERE database NOT IN (\'system\')\nORDER BY tableSchema, tableName',
        'Get all databases (including all tables)', 'ClickHouse', '[]'),
       ('getAllDatabaseAndTable', 'SELECT t.table_schema AS tableSchema, t.table_name AS tableName\nFROM INFORMATION_SCHEMA.TABLES t\nORDER BY t.table_schema, t.table_name',
        'Get all databases (including all tables)', 'MySQL', '[]');
