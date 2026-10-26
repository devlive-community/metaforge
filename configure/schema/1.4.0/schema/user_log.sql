USE `metaforge`;

CREATE TABLE `user_log`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `device`      varchar(255) DEFAULT NULL COMMENT 'Login device',
    `client`      varchar(255) DEFAULT NULL COMMENT 'Login client',
    `ip`          varchar(100) DEFAULT NULL COMMENT 'Login ip',
    `message`     varchar(225) DEFAULT NULL COMMENT 'Error message',
    `state`       varchar(20)  DEFAULT NULL COMMENT 'Login state',
    `ua`          varchar(255) DEFAULT NULL COMMENT 'Trial plug-in, multiple according to, split',
    `user_id`     bigint(20) NOT NULL,
    `create_time` datetime(5)  DEFAULT CURRENT_TIMESTAMP(5),
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT 'User login log';
