package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class Neo4jService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "neo4j:bolt";
    }

    @Override
    public String driver()
    {
        return "org.neo4j.jdbc.Driver";
    }
}
