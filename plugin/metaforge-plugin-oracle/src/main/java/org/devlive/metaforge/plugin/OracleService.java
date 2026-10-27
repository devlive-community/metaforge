package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OracleService
        implements PluginService
{
    @Override
    public String validator()
    {
        return "SELECT version FROM PRODUCT_COMPONENT_VERSION\n" +
                "WHERE product LIKE 'Oracle Database%'";
    }

    @Override
    public String url(RuntimeConfigure configure)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("jdbc:");
        buffer.append(configure.getType());
        buffer.append(":@");
        buffer.append(configure.getHost());
        buffer.append(":");
        buffer.append(configure.getPort());
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
            if (env.size() > 1) {
                buffer.append("?");
            }
            buffer.append(String.join("&", flatEnv));
        }
        return buffer.toString();
    }

    @Override
    public String connectType()
    {
        return "oracle:thin";
    }

    @Override
    public String driver()
    {
        return "oracle.jdbc.OracleDriver";
    }
}
