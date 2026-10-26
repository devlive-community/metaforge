USE
`metaforge`;

-- Executor 命名统一：去掉冗余的 "Executor" 后缀，与 SPI ExecutorService.name() / ExecutorPlugin.getName() 对齐。
-- 例如 'SeatunnelExecutor' -> 'Seatunnel'，'LocalExecutor' -> 'Local'。
-- Unify executor identifier with SPI's name(): strip the redundant "Executor" suffix.

UPDATE `metaforge_dataset`
SET `executor` = 'Local'
WHERE `executor` = 'LocalExecutor';

UPDATE `metaforge_dataset`
SET `executor` = 'Seatunnel'
WHERE `executor` = 'SeatunnelExecutor';

ALTER TABLE `metaforge_dataset`
    ALTER COLUMN `executor` SET DEFAULT 'Local';

UPDATE `metaforge_workflow`
SET `executor` = 'Local'
WHERE `executor` = 'LocalExecutor';

UPDATE `metaforge_workflow`
SET `executor` = 'Seatunnel'
WHERE `executor` = 'SeatunnelExecutor';

-- 同步历史新增 总数 / 已完成 / 进度 三列
-- Sync history adds total count / processed count / progress columns
ALTER TABLE `metaforge_dataset_history`
    ADD COLUMN `total_count`     BIGINT        DEFAULT NULL COMMENT 'Total rows from source query, NULL if pre-count is disabled',
    ADD COLUMN `processed_count` BIGINT        DEFAULT NULL COMMENT 'Rows successfully written to target',
    ADD COLUMN `progress`        DECIMAL(5, 2) DEFAULT NULL COMMENT 'processed_count / total_count * 100, NULL when total unknown';

-- 同步历史新增 任务名 / 工作目录，用于定位 executor 写出的独立任务日志文件
-- Sync history adds task name / work home so the UI can locate the executor's task log file
ALTER TABLE `metaforge_dataset_history`
    ADD COLUMN `task_name` VARCHAR(64)  DEFAULT NULL COMMENT 'Executor task name; also the log file basename',
    ADD COLUMN `work_home` VARCHAR(512) DEFAULT NULL COMMENT 'Executor task workHome; logs live at {work_home}/{task_name}.log';

-- 同步历史新增 effective executor 配置 JSON：本次同步实际生效的所有配置（DB 默认 + 用户临时覆盖合并后）
-- Sync history adds the effective executor configuration JSON used by this run
ALTER TABLE `metaforge_dataset_history`
    ADD COLUMN `executor_configure` TEXT DEFAULT NULL COMMENT 'Effective executor configuration JSON used for this sync';

-- 通用配置表：承载 EXECUTOR / DATASET 等不同范畴的运行时配置
-- Generic runtime configuration table backing metaforge_configure_service.
-- One row per (category, name); configure column is JSON-serialized Map<String,String>.
CREATE TABLE IF NOT EXISTS `metaforge_configure`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(128) NOT NULL COMMENT 'e.g. Local / Seatunnel / Default',
    `code`        VARCHAR(64)  DEFAULT NULL,
    `active`      TINYINT(1)   DEFAULT 1,
    `category`    VARCHAR(32)  NOT NULL COMMENT 'EXECUTOR / DATASET',
    `configure`   TEXT         DEFAULT NULL COMMENT 'JSON-serialized configuration map',
    `description` VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configure_category_name` (`category`, `name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Runtime configuration storage (metaforge_configure)';

-- 新菜单：管理员 → 系统 → 运行时配置（/system/configure）
-- Admin menu entry for the runtime configuration page. Idempotent insert by `code`.
INSERT INTO `metaforge_menu` (`name`, `code`, `description`, `url`, `group_name`, `sorted`, `type`, `parent`, `active`, `i18n_key`, `icon`)
SELECT '管理员 - 系统 - 运行时配置', 'RUNTIME_CONFIGURE',
       '管理员：管理员权限用户可以访问\n位置：顶部管理一级子菜单',
       '/system/configure', NULL, 7, 'VIEW', 8, 1, 'configure.runtime.title', 'Settings2'
WHERE NOT EXISTS (SELECT 1 FROM `metaforge_menu` WHERE `code` = 'RUNTIME_CONFIGURE');

-- 绑定该菜单到 admin 角色（role_id=1）。menu_id 字段为 mediumtext，CAST 后插入。
INSERT INTO `metaforge_role_menu_relation` (`role_id`, `menu_id`)
SELECT '1', CAST(`id` AS CHAR)
FROM `metaforge_menu`
WHERE `code` = 'RUNTIME_CONFIGURE'
  AND NOT EXISTS (
    SELECT 1 FROM `metaforge_role_menu_relation` r
    WHERE r.role_id = '1' AND r.menu_id = CAST(`metaforge_menu`.id AS CHAR)
);
