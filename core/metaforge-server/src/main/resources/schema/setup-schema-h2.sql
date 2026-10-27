CREATE TABLE IF NOT EXISTS `metaforge_configure` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `code` varchar(64) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `category` varchar(32) NOT NULL,
  `configure` text,
  `description` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configure_category_name` (`category`, `name`)
);
CREATE TABLE IF NOT EXISTS `metaforge_chat` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) NOT NULL,
`active` tinyint(1) DEFAULT '1',
`create_time` datetime DEFAULT CURRENT_TIMESTAMP,
`update_time` datetime DEFAULT CURRENT_TIMESTAMP,
`avatar` varchar(255) DEFAULT NULL,
`description` varchar(255) DEFAULT NULL,
`code` varchar(100) DEFAULT NULL,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_chat_user_relation` (
`chat_id` bigint DEFAULT NULL,
`user_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_dashboard` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`active` tinyint(1) DEFAULT '1',
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`configure` longtext,
`description` varchar(1000) DEFAULT NULL,
`code` varchar(100) DEFAULT NULL,
`avatar` text,
`version` varchar(100) DEFAULT '2.0',
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_dashboard_report_relation` (
`dashboard_id` bigint DEFAULT NULL,
`report_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_dashboard_user_relation` (
`dashboard_id` bigint DEFAULT NULL,
`user_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_dataset` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`active` tinyint(1) DEFAULT '1',
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`description` text,
`query` longtext,
`sync_mode` varchar(100) DEFAULT NULL,
`expression` varchar(100) DEFAULT NULL,
`state` varchar(100) DEFAULT NULL,
`message` longtext,
`table_name` varchar(255) DEFAULT NULL,
`code` varchar(100) DEFAULT (uuid()),
`column_mode` varchar(100) DEFAULT 'DIMENSION',
`scheduler` varchar(100) DEFAULT 'LocalScheduler',
`executor` varchar(100) DEFAULT 'Local',
`total_rows` bigint DEFAULT '0',
`total_size` varchar(100) DEFAULT NULL,
`life_cycle` bigint DEFAULT '0',
`life_cycle_column` varchar(100) DEFAULT NULL,
`life_cycle_type` varchar(100) DEFAULT NULL,
`is_custom_column` tinyint(1) DEFAULT '0',
`is_virtual_column` tinyint(1) DEFAULT '0',
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_dataset_column` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`active` tinyint(1) DEFAULT '1',
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`description` text,
`type` varchar(100) DEFAULT NULL,
`comment` varchar(255) DEFAULT NULL,
`original` varchar(255) DEFAULT NULL,
`default_value` varchar(255) DEFAULT NULL,
`position` int DEFAULT NULL,
`is_nullable` tinyint(1) DEFAULT '0',
`length` int DEFAULT NULL,
`dataset_id` int DEFAULT NULL,
`is_order_by_key` tinyint(1) DEFAULT '0',
`column_mode` varchar(100) DEFAULT 'DIMENSION',
`is_partition_key` tinyint(1) DEFAULT '0',
`alias_name` varchar(200) DEFAULT NULL,
`is_primary_key` varchar(100) DEFAULT '0',
`is_sampling_key` tinyint(1) DEFAULT '0',
`is_custom_column` tinyint(1) DEFAULT '0',
`is_virtual_column` tinyint(1) DEFAULT '0',
`code` varchar(100) DEFAULT NULL,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_dataset_column_relation` (
`dataset_id` bigint DEFAULT NULL,
`column_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_dataset_history` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`active` tinyint(1) DEFAULT '1',
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`query` varchar(255) DEFAULT NULL,
`message` text,
`elapsed` bigint DEFAULT NULL,
`count` int DEFAULT NULL,
`total_count` bigint DEFAULT NULL,
`processed_count` bigint DEFAULT NULL,
`progress` decimal(5,2) DEFAULT NULL,
`task_name` varchar(64) DEFAULT NULL,
`work_home` varchar(512) DEFAULT NULL,
`executor_configure` text,
`query_mode` varchar(255) DEFAULT NULL,
`state` varchar(255) DEFAULT NULL,
`code` varchar(100) DEFAULT NULL,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_dataset_history_relation` (
`dataset_history_id` bigint DEFAULT NULL,
`dataset_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_dataset_source_relation` (
`dataset_id` bigint DEFAULT NULL,
`source_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_dataset_user_relation` (
`dataset_id` bigint DEFAULT NULL,
`user_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_function` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`content` varchar(255) DEFAULT NULL,
`description` text,
`plugin` varchar(255) DEFAULT NULL,
`example` text,
`create_time` datetime DEFAULT CURRENT_TIMESTAMP,
`update_time` datetime DEFAULT CURRENT_TIMESTAMP,
`type` varchar(20) DEFAULT 'KEYWORDS',
`active` tinyint(1) DEFAULT '1',
`code` varchar(100) DEFAULT NULL,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_menu` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) NOT NULL,
`code` varchar(255) DEFAULT NULL,
`description` varchar(255) DEFAULT NULL,
`url` varchar(255) NOT NULL,
`group_name` varchar(255) DEFAULT NULL,
`sorted` int DEFAULT '0',
`type` varchar(10) DEFAULT 'VIEW',
`parent` bigint DEFAULT '0',
`active` tinyint(1) DEFAULT '1',
`i18n_key` varchar(255) DEFAULT NULL,
`icon` varchar(255) DEFAULT NULL,
`create_time` datetime DEFAULT CURRENT_TIMESTAMP,
`update_time` datetime DEFAULT CURRENT_TIMESTAMP,
`redirect` bigint DEFAULT '0',
`is_new` tinyint(1) DEFAULT '0',
`view` varchar(550) DEFAULT NULL,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_message` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) NOT NULL,
`active` tinyint(1) DEFAULT '1',
`create_time` datetime DEFAULT CURRENT_TIMESTAMP,
`update_time` datetime DEFAULT CURRENT_TIMESTAMP,
`content` text,
`model` varchar(255) DEFAULT NULL,
`type` varchar(100) DEFAULT NULL,
`prompt_tokens` bigint DEFAULT '0',
`completion_tokens` bigint DEFAULT '0',
`total_tokens` bigint DEFAULT '0',
`code` varchar(100) DEFAULT NULL,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_message_chat_relation` (
`message_id` bigint DEFAULT NULL,
`chat_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_message_user_relation` (
`message_id` bigint DEFAULT NULL,
`user_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_notification` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`code` varchar(255) DEFAULT NULL,
`active` bit(1) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`content` varchar(255) NOT NULL,
`type` varchar(255) DEFAULT NULL,
`is_read` bit(1) NOT NULL DEFAULT FALSE,
`user_id` bigint DEFAULT NULL,
`entity_type` varchar(255) DEFAULT NULL,
`entity_code` varchar(255) DEFAULT NULL,
`entity_name` varchar(255) DEFAULT NULL,
`entity_exists` bit(1) NOT NULL DEFAULT TRUE,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_report` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`active` tinyint(1) DEFAULT '1',
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`configure` longtext,
`type` varchar(255) DEFAULT NULL,
`realtime` tinyint(1) DEFAULT '0',
`query` longtext,
`code` varchar(100) DEFAULT NULL,
`description` varchar(2000) DEFAULT NULL,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_report_dataset_relation` (
`report_id` bigint DEFAULT NULL,
`dataset_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_report_source_relation` (
`report_id` bigint DEFAULT NULL,
`source_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_report_user_relation` (
`report_id` bigint DEFAULT NULL,
`user_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_role` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`description` varchar(255) DEFAULT NULL,
`create_time` datetime(5) DEFAULT CURRENT_TIMESTAMP(5),
`active` tinyint(1) DEFAULT '1',
`code` varchar(100) DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`),
  CONSTRAINT `uk_metaforge_role_code` UNIQUE (`code`)
);
CREATE TABLE IF NOT EXISTS `metaforge_role_menu_relation` (
`role_id` mediumtext,
`menu_id` mediumtext
);
CREATE TABLE IF NOT EXISTS `metaforge_scheduled` (
`id` int NOT NULL AUTO_INCREMENT,
`name` varchar(255) NOT NULL,
`description` text NOT NULL,
`expression` varchar(100) DEFAULT NULL,
`active` tinyint(1) DEFAULT '1',
`is_system` tinyint(1) DEFAULT '1',
`create_time` datetime DEFAULT CURRENT_TIMESTAMP,
`update_time` datetime DEFAULT NULL,
`type` varchar(255) DEFAULT NULL,
`code` varchar(255) DEFAULT NULL,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_scheduled_history` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`active` tinyint(1) DEFAULT '1',
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`info` text,
`state` varchar(100) DEFAULT NULL,
`message` text,
`code` varchar(100) DEFAULT NULL,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_scheduled_history_relation` (
`scheduled_id` int DEFAULT NULL,
`history_id` int DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_scheduled_history_source_relation` (
`scheduled_history_id` bigint DEFAULT NULL,
`source_id` bigint DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_snippet` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`description` varchar(255) DEFAULT NULL,
`context` text,
`create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
`update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
`user_id` bigint NOT NULL,
`code` varchar(100) DEFAULT NULL,
`active` tinyint(1) DEFAULT '1',
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_source` (
`id` bigint NOT NULL AUTO_INCREMENT,
`_catalog` varchar(255) DEFAULT NULL,
`create_time` datetime DEFAULT CURRENT_TIMESTAMP,
`_database` varchar(255) DEFAULT NULL,
`description` varchar(255) DEFAULT NULL,
`host` varchar(255) NOT NULL,
`name` varchar(255) NOT NULL,
`password` varchar(255) DEFAULT NULL,
`port` bigint NOT NULL,
`protocol` varchar(255) DEFAULT NULL,
`username` varchar(255) DEFAULT NULL,
`_type` varchar(100) NOT NULL,
`ssl` tinyint(1) DEFAULT '0',
`_ssl` tinyint(1) DEFAULT '0',
`publish` tinyint(1) DEFAULT '0',
`public` tinyint(1) DEFAULT '0',
`user_id` bigint DEFAULT NULL,
`configure` text,
`used_config` tinyint(1) DEFAULT '0',
`version` varchar(255) DEFAULT NULL,
`available` tinyint(1) DEFAULT '1',
`message` text,
`update_time` datetime DEFAULT NULL,
`code` varchar(100) DEFAULT RANDOM_UUID(),
`active` tinyint(1) DEFAULT '1',
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_source_query` (
`id` bigint NOT NULL AUTO_INCREMENT,
`state` varchar(255) DEFAULT NULL,
`create_time` mediumtext,
`update_time` datetime DEFAULT NULL,
`plugin_id` bigint NOT NULL,
`content` text,
`message` text,
`elapsed` bigint DEFAULT '0',
`user_id` bigint NOT NULL,
`count` bigint DEFAULT '0',
`query_mode` varchar(100) DEFAULT 'ADHOC',
`active` tinyint(1) DEFAULT '1',
`code` varchar(100) DEFAULT NULL,
`name` varchar(100) DEFAULT NULL,
`home` varchar(500) DEFAULT NULL,
`format` varchar(100) DEFAULT 'Json',
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_user` (
`id` bigint NOT NULL AUTO_INCREMENT,
`username` varchar(255) DEFAULT NULL,
`password` varchar(255) DEFAULT NULL,
`create_time` datetime(5) DEFAULT CURRENT_TIMESTAMP(5),
`chat_configure` text,
`is_system` tinyint(1) DEFAULT '0',
`editor_configure` text,
`avatar_configure` longtext,
`active` tinyint(1) DEFAULT '1',
`code` varchar(200) DEFAULT NULL,
`name` varchar(100) DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`notify_configure` text,
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_user_log` (
`id` bigint NOT NULL AUTO_INCREMENT,
`device` varchar(255) DEFAULT NULL,
`client` varchar(255) DEFAULT NULL,
`ip` varchar(100) DEFAULT NULL,
`message` varchar(225) DEFAULT NULL,
`state` varchar(20) DEFAULT NULL,
`ua` varchar(255) DEFAULT NULL,
`user_id` bigint NOT NULL,
`create_time` datetime(5) DEFAULT CURRENT_TIMESTAMP(5),
PRIMARY KEY (`id`)
);
CREATE TABLE IF NOT EXISTS `metaforge_user_role_relation` (
`user_id` bigint NOT NULL,
`role_id` bigint NOT NULL
);
CREATE TABLE IF NOT EXISTS `metaforge_workflow` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`code` varchar(255) DEFAULT NULL,
`active` bit(1) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`update_time` datetime DEFAULT NULL,
`state` varchar(255) DEFAULT NULL,
`message` varchar(255) DEFAULT NULL,
`work` varchar(255) DEFAULT NULL,
`elapsed` bigint DEFAULT NULL,
`executor` varchar(255) DEFAULT NULL,
`configure` text,
`j_from_id` bigint DEFAULT NULL,
`j_to_id` bigint DEFAULT NULL,
`j_user_id` bigint DEFAULT NULL,
PRIMARY KEY (`id`)
);

