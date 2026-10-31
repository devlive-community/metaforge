package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.entity.itransient.user.UserEditorEntity;
import org.devlive.metaforge.service.itransient.NotificationTypeEntity;
import org.devlive.metaforge.service.validation.ValidationGroup;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("metaforge_user")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
// The sources collection is a lazy relation; serializing it outside a session
// blows up responses, and no client consumes it through this entity.
@JsonIgnoreProperties(value = {"sources"})
public class UserEntity
        extends BaseEntity
{
    @NotBlank(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class,
            ValidationGroup.Crud.Auth.class
    })
    @Size(max = 20)
    @Column("username")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String username;

    @NotBlank(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class,
            ValidationGroup.Crud.Auth.class
    })
    @Size(max = 120)
    @Column("password_hash")
    @JsonView(value = {EntityView.NoneView.class})
    private String password;

    @Column("chat_configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String chatConfigure;

    @Column("is_system")
    @JsonView(value = {EntityView.AdminView.class})
    private boolean system;

    @Column("editor_configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private UserEditorEntity editorConfigure;

    @Column("avatar_configure")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private AvatarEntity avatarConfigure;

    // Updated by the sign-in audit handler; null until the first successful sign-in.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Column("last_login_time")
    @JsonView(value = {EntityView.AdminView.class})
    private Date lastLoginTime;

    @Column("last_login_ip")
    @JsonView(value = {EntityView.AdminView.class})
    private String lastLoginIp;

    // Loaded explicitly from metaforge_user_role by the services that need it.
    @Transient
    @JsonIncludeProperties(value = {"name", "code", "description"})
    @JsonView(value = {EntityView.AdminView.class})
    private Set<RoleEntity> roles;

    // JSON array of NotificationTypeEntity; exposed as a list through getNotifyConfigure/setNotifyConfigure.
    @Column("notify_configure")
    @JsonIgnore
    private String notifyConfigureValue;

    // Filled explicitly where the client shows unread notifications.
    @Transient
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long unreadCount;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    public List<NotificationTypeEntity> getNotifyConfigure()
    {
        if (StringUtils.isEmpty(notifyConfigureValue)) {
            return new ArrayList<>();
        }
        try {
            return JsonUtils.objectmapper.readValue(notifyConfigureValue, new TypeReference<List<NotificationTypeEntity>>() {});
        }
        catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setNotifyConfigure(List<NotificationTypeEntity> notifyConfigure)
    {
        this.notifyConfigureValue = JsonUtils.toJSON(notifyConfigure == null ? new ArrayList<>() : notifyConfigure);
    }
}
