package org.devlive.metaforge.plugin.configure;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.view.EntityView;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluginConfigureField
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private PluginConfigureFieldName field; // The field name of the tag configuration

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private PluginConfigureFieldType type;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean required;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long min;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Long max;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String message;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Object value;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean disabled;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private PluginConfigureFieldGroup group = PluginConfigureFieldGroup.configure;

    public static PluginConfigureField of(PluginConfigureFieldName field, PluginConfigureFieldType type, boolean required, Object value, PluginConfigureFieldGroup group)
    {
        PluginConfigureField configureField = PluginConfigureField.builder()
                .field(field)
                .type(type)
                .required(required)
                .build();
        if (value != null) {
            configureField.setValue(value);
        }
        if (group != null) {
            configureField.setGroup(group);
        }
        return configureField;
    }
}
