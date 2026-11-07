package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class MonetDBService
        implements PluginService
{
    @Override
    public String validator()
    {
        // MonetDB has no version() function; the server reports its version in sys.environment
        return "SELECT value AS version FROM sys.environment WHERE name = 'monet_version'";
    }

    @Override
    public String driver()
    {
        return "nl.cwi.monetdb.jdbc.MonetDriver";
    }

    @Override
    public String connectType()
    {
        return "monetdb";
    }
}
