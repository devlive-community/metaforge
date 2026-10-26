-- --------------------------------
--       Update to 1.12.0        --
-- --------------------------------
ALTER TABLE `menus`
    ADD COLUMN `redirect` BIGINT  DEFAULT 0,
    ADD COLUMN `is_new`   BOOLEAN DEFAULT FALSE;

RENAME TABLE `users` TO `metaforge_user`;

ALTER TABLE `metaforge_user`
    ADD COLUMN `is_system` BOOLEAN DEFAULT FALSE;

INSERT INTO `metaforge_user`(`username`, `is_system`)
VALUES ('Ai', TRUE);

CREATE TABLE `metaforge_chat`
(
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name`        VARCHAR(255) NOT NULL,
    `active`      BOOLEAN      DEFAULT TRUE,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `avatar`      VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL
);

CREATE TABLE `metaforge_chat_user_relation`
(
    chat_id BIGINT,
    user_id BIGINT
);

CREATE TABLE `metaforge_message`
(
    `id`                BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name`              VARCHAR(255) NOT NULL,
    `active`            BOOLEAN      DEFAULT TRUE,
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `content`           TEXT         DEFAULT NULL,
    `model`             VARCHAR(255) DEFAULT NULL,
    `type`              VARCHAR(100),
    `prompt_tokens`     BIGINT       DEFAULT 0,
    `completion_tokens` BIGINT       DEFAULT 0,
    `total_tokens`      BIGINT       DEFAULT 0
);

CREATE TABLE `metaforge_message_user_relation`
(
    message_id BIGINT,
    user_id    BIGINT
);

CREATE TABLE `metaforge_message_chat_relation`
(
    message_id BIGINT,
    chat_id    BIGINT
);

ALTER TABLE `metaforge_user`
    CHANGE `third_configure` `chat_configure` text;
