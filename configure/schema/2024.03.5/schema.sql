#
If you are upgrading to 2024.03.4 from a different version, execute the following SQL statement
# Если вы обновляетесь до версии 2024.03.4 с другой версии, выполните следующую инструкцию SQL
# 如果您是通过其他版本升级到 2024.03.4, 请执行以下 SQL 语句

USE `metaforge`;

ALTER TABLE `metaforge_dashboard`
    ADD COLUMN `avatar` TEXT DEFAULT NULL;

ALTER TABLE `metaforge_user`
    ADD COLUMN `active` BOOLEAN DEFAULT TRUE;

ALTER TABLE `metaforge_user`
    ADD COLUMN `code` VARCHAR(200);

ALTER TABLE `metaforge_user`
    ADD COLUMN `name` VARCHAR(100) DEFAULT NULL;

ALTER TABLE `metaforge_user`
    ADD COLUMN `avatar` TEXT DEFAULT NULL;

ALTER TABLE `metaforge_user`
    ADD COLUMN `update_time` DATETIME DEFAULT NULL;
