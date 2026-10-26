USE `metaforge`;

ALTER TABLE `source`
    ADD COLUMN `version`     VARCHAR(255) DEFAULT NULL,
    ADD COLUMN `available`   BOOLEAN      DEFAULT TRUE,
    ADD COLUMN `message`     TEXT         DEFAULT NULL,
    ADD COLUMN `update_time` DATETIME     DEFAULT NULL;

RENAME TABLE `scheduled_task` TO `metaforge_scheduled`;

ALTER TABLE `metaforge_scheduled`
    ADD COLUMN `type` VARCHAR(255) DEFAULT NULL;

UPDATE `metaforge_scheduled`
SET `type` = 'SOURCE_SYNCHRONIZE';

INSERT INTO `metaforge_scheduled` (name, description, expression, active, is_system, type)
VALUES ('Check source available', 'Check the availability of the data source every 1 hour', '0 0 * * * ?', 1, 1, 'SOURCE_CHECK');
