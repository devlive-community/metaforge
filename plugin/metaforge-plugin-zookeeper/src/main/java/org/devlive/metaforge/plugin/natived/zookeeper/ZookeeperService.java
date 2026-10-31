package org.devlive.metaforge.plugin.natived.zookeeper;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.stream.Collectors;

/**
 * ZooKeeper through MetaForge's ZooKeeper JDBC driver: a database is a znode under the root, a
 * table its child and a column the table's child, whose data is the value. Databases, tables and
 * columns come from the driver's JDBC metadata.
 */
public class ZookeeperService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "org.devlive.metaforge.ZookeeperDriver";
    }

    @Override
    public String connectType()
    {
        return "zookeeper";
    }

    /** The host may list an ensemble (h1:2181,h2:2181), so the URL takes it as written. */
    @Override
    public String url(RuntimeConfigure configure)
    {
        if (configure.getUrl().isPresent()) {
            return configure.getUrl().get();
        }
        String host = configure.getHost();
        StringBuilder url = new StringBuilder("jdbc:zookeeper://").append(host);
        if (configure.getPort() != null && !host.contains(":")) {
            url.append(":").append(configure.getPort());
        }
        configure.getDatabase().filter(database -> !database.isEmpty()).ifPresent(database -> url.append("/").append(database));
        configure.getEnv().filter(env -> !env.isEmpty()).ifPresent(env -> url.append("?").append(env.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"))));
        return url.toString();
    }

    @Override
    public Response getDatabases(RuntimeConfigure configure)
    {
        return metadataDatabases(configure);
    }

    @Override
    public Response getTables(RuntimeConfigure configure, String database)
    {
        return metadataTables(configure, database);
    }

    @Override
    public Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        return metadataColumns(configure, database, table);
    }
}
