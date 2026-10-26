package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class PrestoService
        implements PluginService
{
    @Override
    public String validator()
    {
        return "SELECT node_version AS version\n" +
                "FROM system.runtime.nodes\n" +
                "LIMIT 1";
    }

    @Override
    public String connectType()
    {
        return "presto";
    }

    @Override
    public String driver()
    {
        return "io.prestosql.jdbc.PrestoDriver";
    }
}
