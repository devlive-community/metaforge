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
public class PluginConfigureExecutor
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private String executor;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    @Builder.Default
    private RunProtocol protocol = RunProtocol.NONE;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private PluginConfigurePipelineType type;

    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private List<PluginConfigureExecutorField> fields;

    public static PluginConfigureExecutor of(String executor, PluginConfigurePipelineType type, RunProtocol protocol, List<PluginConfigureExecutorField> fields)
    {
        PluginConfigureExecutor configureExecutor = new PluginConfigureExecutor();
        configureExecutor.setExecutor(executor);
        configureExecutor.setType(type);
        configureExecutor.setProtocol(protocol == null ? RunProtocol.NONE : protocol);
        configureExecutor.setFields(fields);
        return configureExecutor;
    }
}
