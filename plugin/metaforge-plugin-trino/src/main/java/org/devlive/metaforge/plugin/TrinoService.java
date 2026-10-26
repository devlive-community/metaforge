package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class TrinoService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "io.trino.jdbc.TrinoDriver";
    }

    @Override
    public String connectType()
    {
        return "trino";
    }
}
