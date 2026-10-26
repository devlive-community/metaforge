package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.devlive.metaforge.common.view.EntityView;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Data
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "metaforge_role")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"},
        justification = "I prefer to suppress these FindBugs warnings")
public class RoleEntity
        extends BaseEntity
{
    @Column(name = "description", columnDefinition = "varchar(1000)")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "metaforge_role_menu_relation",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_id"))
    private Set<MenuEntity> menus = new HashSet<>();
}
