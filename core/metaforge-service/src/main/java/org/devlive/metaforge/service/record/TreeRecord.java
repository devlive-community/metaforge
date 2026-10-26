package org.devlive.metaforge.service.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.entity.MenuEntity;

import java.util.List;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TreeRecord
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    List<TreeRecord> children;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long id;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String title;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String url;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Integer sorted;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String code;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String i18nKey;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String icon;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean isNew;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean checked = false;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Boolean selected = false;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long parent;

    public static TreeRecord of(MenuEntity entity, boolean checked, boolean selected, List<TreeRecord> children)
    {
        return TreeRecord.builder()
                .id(entity.getId())
                .title(entity.getName())
                .url(entity.getUrl())
                .sorted(entity.getSorted())
                .code(entity.getCode())
                .i18nKey(entity.getI18nKey())
                .icon(entity.getIcon())
                .checked(checked)
                .selected(selected)
                .children(children)
                .parent(entity.getParent())
                .build();
    }
}
