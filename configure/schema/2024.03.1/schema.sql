#
If you are upgrading to 2024.03.1 from a different version, execute the following SQL statement
# Если вы обновляетесь до версии 2024.03.1 с другой версии, выполните следующую инструкцию SQL
# 如果您是通过其他版本升级到 2024.03.1, 请执行以下 SQL 语句

USE `metaforge`;

UPDATE `menus`
SET `url` = '/home'
WHERE `id` = 1;

UPDATE `menus`
SET `url` = '/admin/dashboard'
WHERE `id` = 16;

UPDATE `menus`
SET `url` = '/admin/query'
WHERE `id` = 2;

ALTER TABLE `metaforge_source`
    ADD COLUMN `code` VARCHAR(100);

UPDATE `metaforge_source`
SET `code` = REPLACE(UUID(), '-', '')
WHERE `code` IS NULL;
