package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

public class IgniteService
        implements PluginService
{
    @Override
    public String validator()
    {
        return "SELECT VERSION AS version FROM SYS.NODES WHERE IS_CLIENT = FALSE LIMIT 1";
    }

    @Override
    public String connectType()
    {
        return "ignite:thin";
    }

    @Override
    public String driver()
    {
        return "org.apache.ignite.IgniteJdbcThinDriver";
    }

    // Ignite's SQL engine upper-cases unquoted aliases, so information_schema rows come back keyed OBJECT_NAME; JDBC metadata keys them as callers read them
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
