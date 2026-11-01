package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

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
}
