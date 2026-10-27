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
public class SqlServerService
        implements PluginService
{
    @Override
    public String validator()
    {
        return "SELECT @@VERSION";
    }

    @Override
    public String driver()
    {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String connectType()
    {
        return "sqlserver";
    }

    @Override
    public String url(RuntimeConfigure configure)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("jdbc:");
        buffer.append(configure.getType());
        buffer.append("://");
        buffer.append(configure.getHost());
        buffer.append(":");
        buffer.append(configure.getPort());
        buffer.append(";");
        if (configure.getDatabase().isPresent()) {
            buffer.append("databaseName=");
            buffer.append(configure.getDatabase().get());
            buffer.append(";");
        }
        if (configure.getSsl().isPresent()) {
            buffer.append(String.format("ssl=%s", configure.getSsl().get()));
            buffer.append(";");
            buffer.append("trustServerCertificate=true;");
        }
        if (configure.getEnv().isPresent()) {
            Map<String, Object> env = configure.getEnv().get();
            List<String> flatEnv = env.entrySet()
                    .stream()
                    .map(value -> String.format("%s=%s", value.getKey(), value.getValue()))
                    .collect(Collectors.toList());
            buffer.append(String.join(";", flatEnv));
        }
        return buffer.toString();
    }
}
