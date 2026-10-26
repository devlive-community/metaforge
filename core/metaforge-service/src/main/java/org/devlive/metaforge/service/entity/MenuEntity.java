package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.enums.MenuEnum;
import org.devlive.metaforge.common.view.EntityView;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Data
@ToString
@NoArgsConstructor
@Entity
@Table(name = "metaforge_menu")
@EqualsAndHashCode(callSuper = true)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"},
        justification = "I prefer to suppress these FindBugs warnings")
public class MenuEntity
        extends BaseEntity
{
    @Column(name = "description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @Column(name = "url")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String url;

    @Column(name = "group_name")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String group;

    @Column(name = "sorted")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private int sorted;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private MenuEnum type;

    @Column(name = "parent")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private long parent;

    @Column(name = "i18n_key")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String i18nKey;

    @Column(name = "icon")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String icon;

    @Column(name = "redirect")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private long redirect;

    @Column(name = "is_new")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean isNew;
}
