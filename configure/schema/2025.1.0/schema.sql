USE
`metaforge`;

DROP TABLE IF EXISTS `metaforge_metadata_table_database_relation`;
DROP TABLE IF EXISTS `metaforge_metadata_table`;
DROP TABLE IF EXISTS `metaforge_metadata_database_source_relation`;
DROP TABLE IF EXISTS `metaforge_metadata_database`;
DROP TABLE IF EXISTS `metaforge_metadata_column_table_relation`;
DROP TABLE IF EXISTS `metaforge_metadata_column`;
DROP TABLE IF EXISTS `metaforge_template`;

DELETE
FROM `metaforge_menu`
WHERE id = 11;

CREATE TABLE metaforge_notification
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    name          VARCHAR(255) NULL,
    code          VARCHAR(255) NULL,
    active        BIT(1) NULL,
    create_time   datetime NULL,
    update_time   datetime NULL,
    content       VARCHAR(255) NOT NULL,
    type          VARCHAR(255) NULL,
    is_read       BIT(1)                DEFAULT 0 NOT NULL,
    user_id       BIGINT NULL,
    entity_type   VARCHAR(255) NULL,
    entity_code   VARCHAR(255) NULL,
    entity_name   VARCHAR(255) NULL,
    entity_exists BIT(1)       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_metaforge_notification PRIMARY KEY (id)
);

ALTER TABLE `metaforge_notification`
    ADD CONSTRAINT FK_METAFORGE_NOTIFICATION_ON_USER
        FOREIGN KEY (user_id)
            REFERENCES metaforge_user (id)
            ON DELETE CASCADE;

ALTER TABLE `metaforge_user`
    ADD NOTIFICATION_TYPES VARCHAR(200);
