USE `metaforge`;

TRUNCATE TABLE `metaforge_source_query`;

TRUNCATE TABLE `metaforge_chat`;
TRUNCATE TABLE `metaforge_chat_user_relation`;

TRUNCATE TABLE `metaforge_dashboard`;
TRUNCATE TABLE `metaforge_dashboard_report_relation`;
TRUNCATE TABLE `metaforge_dashboard_user_relation`;

TRUNCATE TABLE `metaforge_dataset`;
TRUNCATE TABLE `metaforge_dataset_column`;
TRUNCATE TABLE `metaforge_dataset_column_relation`;
TRUNCATE TABLE `metaforge_dataset_source_relation`;
TRUNCATE TABLE `metaforge_dataset_user_relation`;
TRUNCATE TABLE `metaforge_dataset_history`;
TRUNCATE TABLE `metaforge_dataset_history_relation`;

TRUNCATE TABLE `metaforge_message`;
TRUNCATE TABLE `metaforge_message_chat_relation`;
TRUNCATE TABLE `metaforge_message_user_relation`;

TRUNCATE TABLE `metaforge_report`;
TRUNCATE TABLE `metaforge_report_source_relation`;
TRUNCATE TABLE `metaforge_report_user_relation`;
TRUNCATE TABLE `metaforge_report_dataset_relation`;

TRUNCATE TABLE `metaforge_scheduled_history`;
TRUNCATE TABLE `metaforge_scheduled_history_relation`;
TRUNCATE TABLE `metaforge_scheduled_history_source_relation`;

TRUNCATE TABLE `metaforge_user_log`;

TRUNCATE TABLE `metaforge_source`;

TRUNCATE TABLE `metaforge_snippet`;

TRUNCATE TABLE `metaforge_workflow`;

TRUNCATE TABLE `metaforge_notification`;

mysqldump -u root -p metaforge > $(pwd)/configure/schema/metaforge.sql
