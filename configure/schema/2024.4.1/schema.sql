CREATE TABLE metaforge_workflow
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    name        VARCHAR(255) NULL,
    code        VARCHAR(255) NULL,
    active      BIT(1) NULL,
    create_time datetime NULL,
    update_time datetime NULL,
    state       VARCHAR(255) NULL,
    message     VARCHAR(255) NULL,
    work        VARCHAR(255) NULL,
    elapsed     BIGINT NULL,
    executor    VARCHAR(255) NULL,
    configure   TEXT NULL,
    j_from_id   BIGINT NULL,
    j_to_id     BIGINT NULL,
    j_user_id   BIGINT NULL,
    CONSTRAINT pk_metaforge_workflow PRIMARY KEY (id)
);

INSERT INTO metaforge_menu (id, name, code, description, url, group_name, sorted, type, parent, active, i18n_key,
                          icon, create_time, update_time, redirect, is_new, view)
VALUES (21, '管理员 - 管理 - 工作流', 'b6669c359a7d41e581d906446366daf3', '', '/admin/workflow', 'default', 1, 'VIEW', 3, 1,
        'common.workflow', 'Workflow', '2024-12-19 18:36:22', '2024-12-19 18:36:22', 0, 1, null);

INSERT INTO metaforge_role_menu_relation (role_id, menu_id)
VALUES ('1', '21');


