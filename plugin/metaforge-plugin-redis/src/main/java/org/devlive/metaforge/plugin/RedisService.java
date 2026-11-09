package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

/**
 * Redis is browsed like any other source: the numbered databases, their keys as tables, and key,
 * type and value as columns, all from the Redis JDBC driver's metadata.
 */
public class RedisService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "redis";
    }

    @Override
    public String driver()
    {
        return "org.devlive.metaforge.RedisDriver";
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
