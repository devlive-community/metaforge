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
import java.util.Map;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "EI_EXPOSE_REP2", "EI_EXPOSE_REP", "NP_NULL_PARAM_DEREF_NONVIRTUAL"},
        justification = "I prefer to suppress these FindBugs warnings")
public class PluginConfigure
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String name;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private java.util.Date supportTime;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<PluginConfigureField> configures;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<PluginConfigureExecutor> pipelines;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private PluginConfigureCategory category = PluginConfigureCategory.OTHER;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private Map<String, String> descriptions = java.util.Collections.emptyMap();

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private Map<String, List<String>> features = java.util.Collections.emptyMap();

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private Map<String, List<String>> scenes = java.util.Collections.emptyMap();

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private boolean popular = false;

    public static PluginConfigure of(String name, List<PluginConfigureField> configures, List<PluginConfigureExecutor> pipelines)
    {
        PluginConfigure configure = new PluginConfigure();
        configure.setName(name);
        configure.setConfigures(configures);
        configure.setPipelines(pipelines);
        return configure;
    }
}
