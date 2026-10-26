package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.enums.EntityType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(value = AuditingEntityListener.class)
@Table(name = "metaforge_notification")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class NotificationEntity
        extends BaseEntity
{
    @Column(nullable = false)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String content;

    @Column(name = "type")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String type;

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean isRead = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEntity user;

    @Column(name = "entity_type")
    @Enumerated(EnumType.STRING)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private EntityType entityType;

    @Column(name = "entity_code")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String entityCode;    // 实体编码

    @Column(name = "entity_name")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String entityName;    // 实体名称

    @Column(name = "entity_exists", nullable = false, columnDefinition = "boolean default true")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean entityExists = true;
}
