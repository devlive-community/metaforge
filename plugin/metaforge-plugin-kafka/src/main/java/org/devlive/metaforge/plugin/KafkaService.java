package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.stream.Collectors;

/**
 * Kafka through MetaForge's Kafka JDBC driver: the cluster is the database {@code kafka}, topics
 * are tables and records rows with the columns partition, offset, timestamp, key and value.
 * Databases, tables and columns come from the driver's JDBC metadata.
 */
public class KafkaService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "org.devlive.metaforge.KafkaDriver";
    }

    @Override
    public String connectType()
    {
        return "kafka";
    }

    /** The host lists the bootstrap servers (b1:9092,b2:9092), so the URL takes it as written. */
    @Override
    public String url(RuntimeConfigure configure)
    {
        if (configure.getUrl().isPresent()) {
            return configure.getUrl().get();
        }
        String host = configure.getHost();
        StringBuilder url = new StringBuilder("jdbc:kafka://").append(host);
        if (configure.getPort() != null && !host.contains(":")) {
            url.append(":").append(configure.getPort());
        }
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
