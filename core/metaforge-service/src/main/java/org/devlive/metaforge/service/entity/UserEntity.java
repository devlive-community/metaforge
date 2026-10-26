package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.converter.AvatarConverter;
import org.devlive.metaforge.service.converter.NotificationTypeConverter;
import org.devlive.metaforge.service.converter.UserEditorConverter;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.entity.itransient.user.UserEditorEntity;
import org.devlive.metaforge.service.itransient.NotificationTypeEntity;
import org.devlive.metaforge.service.validation.ValidationGroup;
import org.hibernate.annotations.Formula;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "metaforge_user")
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
public class UserEntity
        extends BaseEntity
{
    @NotBlank(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class,
            ValidationGroup.Crud.Auth.class
    })
    @Size(max = 20)
    @Column(name = "username")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String username;

    @NotBlank(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class,
            ValidationGroup.Crud.Auth.class
    })
    @Size(max = 120)
    @Column(name = "password")
    @JsonView(value = {EntityView.NoneView.class})
    private String password;

    @Column(name = "chat_configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String chatConfigure;

    @Column(name = "is_system")
    @JsonView(value = {EntityView.AdminView.class})
    private boolean system;

    @Column(name = "editor_configure")
    @Convert(converter = UserEditorConverter.class)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEditorEntity editorConfigure;

    @Column(name = "avatar_configure")
    @Convert(converter = AvatarConverter.class)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private AvatarEntity avatarConfigure;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "metaforge_user_role_relation",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonIncludeProperties(value = {"name", "code", "description"})
    @JsonView(value = {EntityView.AdminView.class})
    private Set<RoleEntity> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @JsonView(value = {EntityView.NoneView.class})
    @JsonIgnore
    private List<SourceEntity> sources;

    @Column(name = "notify_configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Convert(converter = NotificationTypeConverter.class)
    private List<NotificationTypeEntity> notifyConfigure = Lists.newArrayList();

    @Formula("(SELECT COUNT(n.id) FROM metaforge_notification n WHERE n.user_id = id AND n.is_read = false)")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long unreadCount;
}
