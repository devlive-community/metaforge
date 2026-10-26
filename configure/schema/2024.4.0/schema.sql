USE
`metaforge`;

ALTER TABLE `metaforge_dashboard`
    ADD COLUMN `version` VARCHAR(100) DEFAULT NULL;

UPDATE `metaforge_dashboard`
SET `version` = '1.0'
WHERE `version` IS NULL;

UPDATE `metaforge_dataset`
SET `executor` = 'LocalExecutor'
WHERE `executor` = 'Default';

ALTER TABLE `metaforge_dataset`
    ALTER COLUMN `executor` SET DEFAULT 'LocalExecutor';

UPDATE `metaforge_dataset`
SET `scheduler` = 'LocalScheduler'
WHERE `scheduler` = 'Default';

ALTER TABLE `metaforge_dataset`
    ALTER COLUMN `scheduler` SET DEFAULT 'LocalScheduler';
