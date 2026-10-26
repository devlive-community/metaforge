# If you are upgrading to 2024.02.1 from a different version, execute the following SQL statement
# Если вы обновляетесь до версии 2024.02.1 с другой версии, выполните следующую инструкцию SQL
# 如果您是通过其他版本升级到 2024.02.1, 请执行以下 SQL 语句

USE `metaforge`;

ALTER TABLE `metaforge_dataset_column`
    ADD COLUMN `is_primary_key`    BOOLEAN DEFAULT FALSE,
    ADD COLUMN `is_sampling_key`   BOOLEAN DEFAULT FALSE,
    ADD COLUMN `is_custom_column`  BOOLEAN DEFAULT FALSE,
    ADD COLUMN `is_virtual_column` BOOLEAN DEFAULT FALSE;

ALTER TABLE `metaforge_dataset`
    ADD COLUMN `total_rows`        BIGINT(100) DEFAULT 0,
    ADD COLUMN `total_size`        VARCHAR(100),
    ADD COLUMN `life_cycle`        BIGINT(100) DEFAULT 0,
    ADD COLUMN `life_cycle_column` VARCHAR(100),
    ADD COLUMN `life_cycle_type`   VARCHAR(100);
