package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.persistence.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Set;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("metaforge_dashboard")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class DashboardEntity
        extends BaseEntity
{
    @Column("configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String configure;

    @Column("description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @Column("avatar")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private AvatarEntity avatar;

    @Column("version")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String version;

    @Column("user_id")
    @JsonView(value = {EntityView.AdminView.class})
    private Long userId;

    @Transient
    @Reference("userId")
    @JsonIncludeProperties(value = {"name", "username", "code"})
    @JsonView(value = {EntityView.AdminView.class})
    private UserEntity user;

    @Transient
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Set<ReportEntity> reports;
}
