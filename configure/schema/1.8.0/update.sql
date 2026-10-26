create table `user_chat`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT,
    `name`        varchar(255) NOT NULL,
    `question`    text         NOT NULL,
    `answer`      text     DEFAULT NULL,
    `type`        varchar(100),
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `end_time`    datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `elapsed`     bigint,
    `user_id`     int(11)      NOT NULL,
    `is_new`      boolean  default true,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

create table `scheduled_task`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT,
    `name`        varchar(255) NOT NULL,
    `description` text         NOT NULL,
    `expression`  varchar(100),
    `active`      boolean  default true,
    `is_system`   boolean  default true,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

INSERT INTO metaforge.scheduled_task (name, description, expression, active, is_system)
VALUES ('Synchronize table structure', 'Synchronize the table structure of the data source library at 1 am every day', '0 0 1 * * ?', 1, 1);
