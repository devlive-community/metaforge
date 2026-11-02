package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.connection.JdbcUrlGuard;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.ArrayList;
import java.util.List;

public class YDBService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "ydb";
    }

    @Override
    public String driver()
    {
        return "com.yandex.ydb.jdbc.YdbDriver";
    }

    /** The driver reads jdbc:ydb:host:port/database; with // after the scheme it finds no host. */
    @Override
    public String url(RuntimeConfigure configure)
    {
        if (configure.getUrl().isPresent()) {
            return configure.getUrl().get();
        }
        StringBuilder url = new StringBuilder("jdbc:ydb:")
                .append(JdbcUrlGuard.validateHost(configure.getHost()))
                .append(":")
                .append(configure.getPort());
        configure.getDatabase().ifPresent(database -> {
            JdbcUrlGuard.validateDatabase(database);
            url.append("/").append(database);
        });
        List<String> parameters = new ArrayList<>();
        configure.getEnv().ifPresent(env -> env.forEach((key, value) -> {
            JdbcUrlGuard.validateParameter(key, String.valueOf(value));
            parameters.add(key + "=" + value);
        }));
        if (!parameters.isEmpty()) {
            url.append("?").append(String.join("&", parameters));
        }
        return url.toString();
    }
}
