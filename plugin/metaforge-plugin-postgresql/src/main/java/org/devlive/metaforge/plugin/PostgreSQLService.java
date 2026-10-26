package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class PostgreSQLService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "org.postgresql.Driver";
    }

    @Override
    public String connectType()
    {
        return "postgresql";
    }
}
