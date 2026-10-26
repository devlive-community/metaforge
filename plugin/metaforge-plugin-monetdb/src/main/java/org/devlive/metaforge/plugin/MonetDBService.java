package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class MonetDBService
        implements PluginService
{
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
