USE
`metaforge`;

ALTER TABLE `metaforge_user`
    CHANGE notification_types notify_configure TEXT NULL;

DROP TABLE `metaforge_pipeline`;
DROP TABLE `metaforge_pipeline_user_relation`;

DELETE
FROM `metaforge_menu`
WHERE id = 7;