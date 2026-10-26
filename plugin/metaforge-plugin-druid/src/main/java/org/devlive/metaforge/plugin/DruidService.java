package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Configure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class DruidService
        implements PluginService
{
    @Override
    public String validator()
    {
        return "SELECT '-' AS version";
    }

    @Override
    public String url(Configure configure)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("jdbc:");
        buffer.append(configure.getType());
        buffer.append("://");
        buffer.append(configure.getHost());
        buffer.append(":");
        buffer.append(configure.getPort());
        buffer.append("/druid/v2/sql/avatica");
        if (configure.getDatabase().isPresent()) {
            buffer.append("/");
            buffer.append(configure.getDatabase().get());
        }
        if (configure.getEnv().isPresent()) {
            Map<String, Object> env = configure.getEnv().get();
            List<String> flatEnv = env.entrySet()
                    .stream()
                    .map(value -> String.format("%s=%s", value.getKey(), value.getValue()))
                    .collect(Collectors.toList());
            buffer.append("?");
            buffer.append(String.join("&", flatEnv));
        }
        return buffer.toString();
    }

    @Override
    public String connectType()
    {
        return "avatica:remote:url=http";
    }

    @Override
    public String driver()
    {
        return "org.apache.calcite.avatica.remote.Driver";
    }
}
