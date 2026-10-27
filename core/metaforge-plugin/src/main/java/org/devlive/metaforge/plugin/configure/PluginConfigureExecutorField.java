package org.devlive.metaforge.plugin.configure;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.common.view.EntityView;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class PluginConfigureExecutorField
{
    // The field name
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String field;

    // The default is equal to the filed value, and the custom column name uses
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String origin;

    // When the value is true, it means that the field is required
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private boolean required;

    // If the flag is true
    // it means that the field is extracted through user configuration, and the default data will be discarded
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private boolean override = false;

    // Is it an input parameter
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private boolean input = false;

    // Related component width
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private int width = 300;

    // Component type
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private FieldType type = FieldType.INPUT;

    // User-defined prompts
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String tooltip;

    // The result of the current configuration input
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private Object value;

    // If this configuration item is true, it will not be displayed on the front end and will be displayed after it is enabled.
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private boolean hidden = false;

    // The description
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String description;

    // If the type is SELECT , the default data passed in is required
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<String> defaultValues;

    // If it is auto-generated, the format is：protocol://host:port
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private boolean generated = false;
}
