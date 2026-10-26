USE metaforge;

DROP TABLE IF EXISTS `template_sql`;
CREATE TABLE `template_sql`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `name`        varchar(255)        DEFAULT NULL COMMENT 'Name of template',
    `content`     varchar(255)        DEFAULT NULL COMMENT 'Template SQL code',
    `description` varchar(255)        DEFAULT NULL COMMENT 'Template description',
    `plugin`      varchar(255)        DEFAULT NULL COMMENT 'Using plug-ins',
    `configure`   text                DEFAULT NULL COMMENT 'The template must use the configuration information in the key->value format',
    `create_time` timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT 'The system preset SQL template table';
