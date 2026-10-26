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
import org.devlive.metaforge.service.converter.AvatarConverter;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.util.Set;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "metaforge_dashboard")
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class DashboardEntity
        extends BaseEntity
{
    @Column(name = "configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String configure;

    @Column(name = "description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @Column(name = "avatar")
    @Convert(converter = AvatarConverter.class)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private AvatarEntity avatar;

    @Column(name = "version")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String version;

    @ManyToOne
    @JoinTable(name = "metaforge_dashboard_user_relation",
            joinColumns = @JoinColumn(name = "dashboard_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIncludeProperties(value = {"name", "username", "code"})
    @JsonView(value = {EntityView.AdminView.class})
    private UserEntity user;

    @ManyToMany
    @JoinTable(name = "metaforge_dashboard_report_relation",
            joinColumns = @JoinColumn(name = "dashboard_id"),
            inverseJoinColumns = @JoinColumn(name = "report_id"))
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Set<ReportEntity> reports;
}
