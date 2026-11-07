package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

public class H2TCPService
        implements PluginService
{
    @Override
    public String validator()
    {
        return "SELECT SETTING_VALUE AS version\n" +
                "FROM INFORMATION_SCHEMA.SETTINGS\n" +
                "WHERE SETTING_NAME  = 'info.VERSION'";
    }

    @Override
    public String driver()
    {
        return "org.h2.Driver";
    }

    @Override
    public String connectType()
    {
        return "h2:tcp";
    }

    // H2 upper-cases unquoted aliases, so information_schema rows come back keyed OBJECT_NAME; JDBC metadata keys them as callers read them
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
