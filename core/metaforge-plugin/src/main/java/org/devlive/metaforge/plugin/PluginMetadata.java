package org.devlive.metaforge.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class PluginMetadata
{
    private String name;
    private String version;
    private PluginState state;
    private long loadTimestamp;
    private String loadTime;
    private PluginType type;
    private String loaderName;
    private String key;
    private Object configure;

    @JsonIgnore
    private ClassLoader classLoader;

    @JsonIgnore
    private Object instance;

    @JsonIgnore
    private Path location;
}
