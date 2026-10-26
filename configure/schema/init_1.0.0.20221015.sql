CREATE DATABASE IF NOT EXISTS metaforge;

USE metaforge;

-- ----------------------------
-- Table structure for audit_plugin
-- ----------------------------
CREATE TABLE IF NOT EXISTS `audit_plugin`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `state`       varchar(255) COLLATE utf8_bin DEFAULT NULL,
    `create_time` datetime(5)                   DEFAULT CURRENT_TIMESTAMP(5),
    `end_time`    datetime(5)                   DEFAULT CURRENT_TIMESTAMP(5),
    `plugin_id`   bigint(20) NOT NULL,
    `content`     text COLLATE utf8_bin,
    `message`     text COLLATE utf8_bin,
    PRIMARY KEY (`id`)
) DEFAULT CHARSET = utf8
  COLLATE = utf8_bin;

-- ----------------------------
-- Table structure for source
-- ----------------------------
CREATE TABLE IF NOT EXISTS `source`
(
    `id`          bigint(20)                    NOT NULL AUTO_INCREMENT,
    `_catalog`    varchar(255) COLLATE utf8_bin DEFAULT NULL,
    `create_time` datetime                      DEFAULT CURRENT_TIMESTAMP,
    `_database`   varchar(255) COLLATE utf8_bin DEFAULT NULL,
    `description` varchar(255) COLLATE utf8_bin DEFAULT NULL,
    `host`        varchar(255) COLLATE utf8_bin NOT NULL,
    `name`        varchar(255) COLLATE utf8_bin NOT NULL,
    `password`    varchar(255) COLLATE utf8_bin DEFAULT NULL,
    `port`        bigint(20)                    NOT NULL,
    `protocol`    varchar(255) COLLATE utf8_bin DEFAULT NULL,
    `username`    varchar(255) COLLATE utf8_bin DEFAULT NULL,
    `_type`       varchar(100) COLLATE utf8_bin NOT NULL,
    PRIMARY KEY (`id`)
) DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='The storage is used to query the data connection source';
