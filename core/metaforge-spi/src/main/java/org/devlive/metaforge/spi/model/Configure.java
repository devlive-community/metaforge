package org.devlive.metaforge.spi.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;

import java.util.Map;
import java.util.Optional;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "CT_CONSTRUCTOR_THROW",
        "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"})
public class Configure
        implements Cloneable
{
    @NonNull
    private Plugin plugin;
    private PluginManager pluginManager;
    private String host;
    private Integer port;

    @Builder.Default
    private Optional<String> username = Optional.empty();

    @Builder.Default
    private Optional<String> password = Optional.empty();

    @Builder.Default
    private Optional<String> database = Optional.empty();

    @Builder.Default
    private Optional<String> version = Optional.empty();

    @Builder.Default
    private Optional<Map<String, Object>> env = Optional.empty();

    @Builder.Default
    private Optional<Boolean> ssl = Optional.empty();

    @Builder.Default
    private String format = "JsonConvert";
    // if `to`: skip
    private Optional<String> query = Optional.empty();
    // Support for custom upload configuration plugins
    private String home;
    private boolean usedConfig;
    private String id;

    // 自定义 url
    // Custom url
    @Builder.Default
    private Optional<String> url = Optional.empty();

    private String driver;
    private String type;

    @Builder.Default
    private Boolean isAppendChar = Boolean.TRUE;

    @Override
    public Configure clone()
    {
        try {
            return (Configure) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
