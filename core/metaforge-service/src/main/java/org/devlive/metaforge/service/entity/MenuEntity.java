package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.enums.MenuEnum;
import org.devlive.metaforge.common.view.EntityView;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@NoArgsConstructor
@Table("metaforge_menu")
@EqualsAndHashCode(callSuper = true)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"},
        justification = "I prefer to suppress these FindBugs warnings")
public class MenuEntity
        extends BaseEntity
{
    @Column("description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @Column("url")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String url;

    @Column("group_name")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String group;

    @Column("sorted")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private int sorted;

    @Column("type")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private MenuEnum type;

    @Column("parent")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private long parent;

    @Column("i18n_key")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String i18nKey;

    @Column("icon")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String icon;

    @Column("redirect")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private long redirect;

    @Column("is_new")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean isNew;
}
