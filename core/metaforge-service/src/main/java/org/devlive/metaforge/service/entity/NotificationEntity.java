package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("metaforge_notification")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class NotificationEntity
        extends BaseEntity
{
    @Column("content")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String content;

    @Column("type")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String type;

    @Column("is_read")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean isRead = false;

    @Column("user_id")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long userId;

    @Transient
    @Reference("userId")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;

    @Column("entity_type")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private EntityType entityType;

    @Column("entity_code")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String entityCode;    // 实体编码

    @Column("entity_name")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String entityName;    // 实体名称

    @Column("entity_exists")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean entityExists = true;
}
