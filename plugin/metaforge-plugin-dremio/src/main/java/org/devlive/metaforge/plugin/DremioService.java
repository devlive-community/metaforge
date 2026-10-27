package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class DremioService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "com.dremio.jdbc.Driver";
    }

    @Override
    public String connectType()
    {
        return "dremio:direct";
    }

    @Override
    public String url(RuntimeConfigure configure)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("jdbc:");
        buffer.append(configure.getType());
        buffer.append("=");
        buffer.append(configure.getHost());
        buffer.append(":");
        buffer.append(configure.getPort());
        if (configure.getDatabase().isPresent()) {
            buffer.append(";");
            buffer.append(configure.getDatabase().get());
        }
        if (configure.getSsl().isPresent()) {
            if (configure.getSsl().get()) {
                buffer.append(String.format("?ssl=%s", configure.getSsl().get()));
            }
        }
        if (configure.getEnv().isPresent()) {
            Map<String, Object> env = configure.getEnv().get();
            List<String> flatEnv = env.entrySet()
                    .stream()
                    .map(value -> String.format("%s=%s", value.getKey(), value.getValue()))
                    .collect(Collectors.toList());
            if (!configure.getSsl().isPresent()) {
                buffer.append("?");
            }
            else {
                buffer.append("&");
            }
            buffer.append(String.join("&", flatEnv));
        }
        return buffer.toString();
    }
}
