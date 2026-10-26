USE `metaforge`;

ALTER TABLE `metaforge_source_query`
    ADD COLUMN `format` VARCHAR(100) DEFAULT 'Json';
