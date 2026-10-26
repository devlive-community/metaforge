USE `metaforge`;

UPDATE `template_sql`
SET `system` = true
WHERE `name` = 'getAllDatabaseAndTable';

INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`, `system`)
VALUES ('getAllRunningProcess',
        'SELECT\n  query_id AS id,\n  query AS content,\n  toUInt64(toUInt64(read_rows) + toUInt64(written_rows)) AS rows,\n  round(elapsed, 1) AS elapsed,\n  formatReadableSize(toUInt64(read_bytes) + toUInt64(written_bytes)) AS bytes,\n  formatReadableSize(memory_usage) AS memory,\n  formatReadableSize(read_bytes) AS bytesRead,\n  formatReadableSize(written_bytes) AS bytesWritten\nFROM\n  system.processes\nWHERE\n  round(elapsed, 1) > 0',
        'Get a running list of all currently specified server nodes', 'ClickHouse', '[]', true);
INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`, `system`)
VALUES ('getAllRunningProcess',
        'SELECT\n  id AS id,\n  info AS content,\n  \'-\' AS rows,\n  time AS elapsed,\n  \'-\' AS bytes,\n  \'-\' AS memory,\n  \'-\' AS bytesRead,\n  \'-\' AS bytesWritten\nFROM\n  information_schema.PROCESSLIST',
        'Get a running list of all currently specified server nodes', 'MySQL', '[]', true);
INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`, `system`)
VALUES ('getAllColumnsFromDatabaseAndTable', 'DESC TABLE ${table:String}', 'Get the data column from the database and table', 'MySQL,ClickHouse',
        '[{\"column\":\"table\",\"type\":\"String\",\"expression\":\"${table:String}\"}]', true);
INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`, `system`)
VALUES ('getDataFromDatabaseAndTableLimited', 'SELECT *\nFROM ${table:String}\nLIMIT ${size:Integer}\nOFFSET ${page:Integer}', 'Get all data from table by limited',
        'MySQL,ClickHouse',
        '[{\"column\":\"table\",\"type\":\"String\",\"expression\":\"${table:String}\"},{\"column\":\"size\",\"type\":\"Integer\",\"expression\":\"${size:Integer}\"},{\"column\":\"page\",\"type\":\"Integer\",\"expression\":\"${page:Integer}\"}]',
        true);
INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`, `system`)
VALUES ('getAllDatabase', 'SHOW DATABASES', 'Gets a list of all databases', 'ClickHouse,MySQL', '[]', true);
INSERT INTO `template_sql` (`name`, `content`, `description`, `plugin`, `configure`, `system`)
VALUES ('getAllTablesFromDatabase', 'SHOW TABLES FROM ${database:String}', 'Get the data table from the database', 'ClickHouse,MySQL',
        '[{\"column\":\"database\",\"type\":\"String\",\"expression\":\"${database:String}\"}]', true);
