package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.connection.JdbcUrlGuard;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class YDBService
        implements PluginService
{
    // YQL schema statements; the driver runs anything else as a data query, which rejects them
    private static final Pattern SCHEME_STATEMENT = Pattern.compile("^\\s*(CREATE|ALTER|DROP)\\s", Pattern.CASE_INSENSITIVE);
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
        // The driver leaves autocommit off, and each query runs on its own connection, so writes would never be committed
        if (configure.getEnv().map(env -> !env.containsKey("autoCommit")).orElse(true)) {
            parameters.add("autoCommit=true");
        }
        configure.getEnv().ifPresent(env -> env.forEach((key, value) -> {
            JdbcUrlGuard.validateParameter(key, String.valueOf(value));
            parameters.add(key + "=" + value);
        }));
        if (!parameters.isEmpty()) {
            url.append("?").append(String.join("&", parameters));
        }
        return url.toString();
    }

    /** Runs schema statements as scheme queries, with the driver's --jdbc:SCHEME hint, unless the statement names a mode itself. */
    @Override
    public Response execute(RuntimeConfigure configure, String content)
    {
        if (content != null && !content.contains("--jdbc:") && SCHEME_STATEMENT.matcher(content).find()) {
            return PluginService.super.execute(configure, "--jdbc:SCHEME\n" + content);
        }
        return PluginService.super.execute(configure, content);
    }
}
