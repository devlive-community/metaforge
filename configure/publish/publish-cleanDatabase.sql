-- Clears business data from a metadata database before publishing a demo snapshot.
-- Plain SQL that runs on every supported database; the catalogue (users, roles,
-- menus, functions, schedules) and the changelog history are kept.

DELETE FROM metaforge_source_query;

DELETE FROM metaforge_message;
DELETE FROM metaforge_chat;

DELETE FROM metaforge_dashboard_report;
DELETE FROM metaforge_dashboard;

DELETE FROM metaforge_report;

DELETE FROM metaforge_dataset_column;
DELETE FROM metaforge_dataset_history;
DELETE FROM metaforge_dataset;

DELETE FROM metaforge_scheduled_history;

DELETE FROM metaforge_user_log;

DELETE FROM metaforge_source;

DELETE FROM metaforge_snippet;

DELETE FROM metaforge_workflow;

DELETE FROM metaforge_notification;
