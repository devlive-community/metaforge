package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.enums.FunctionType;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.validation.ValidationGroup;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.util.Arrays;
import java.util.List;

@Data
@ToString
@NoArgsConstructor
@Table("metaforge_function")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"},
        justification = "I prefer to suppress these FindBugs warnings")
public class FunctionEntity
        extends BaseEntity
{
    @NotBlank(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class
    })
    @Column("content")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String content;

    @NotBlank(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class
    })
    @Column("description")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @NotNull(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class
    })
    @Size(min = 1)
    @Column("plugin")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String plugin;

    @NotBlank(groups = {
            ValidationGroup.Crud.Create.class,
            ValidationGroup.Crud.Update.class
    })
    @Column("example")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String example;

    @Column("type")
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private FunctionType type = FunctionType.KEYWORD;

    public List<String> getPlugin()
    {
        return Arrays.asList(plugin.split(","));
    }

    public void setPlugin(List<String> plugin)
    {
        this.plugin = String.join(",", plugin);
    }
}
