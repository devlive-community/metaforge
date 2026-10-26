-- If it already contains historical data, please use the following script to fix it
ALTER TABLE `metaforge`.`audit_plugin`
    MODIFY COLUMN `create_time` timestamp(5) NULL DEFAULT CURRENT_TIMESTAMP(5),
    MODIFY COLUMN `end_time` timestamp(5) NULL DEFAULT CURRENT_TIMESTAMP(5);
ALTER TABLE `metaforge`.`audit_plugin`
    ADD COLUMN `elapsed` bigint NULL DEFAULT 0;
