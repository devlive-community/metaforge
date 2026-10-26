USE `metaforge`;

ALTER TABLE `template_sql`
    MODIFY COLUMN `content` text,
    MODIFY COLUMN `description` text,
    ADD COLUMN `system` BOOLEAN DEFAULT FALSE;
