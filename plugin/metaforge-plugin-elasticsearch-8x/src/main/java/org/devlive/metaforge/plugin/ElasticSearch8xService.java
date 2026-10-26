package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class ElasticSearch8xService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "es";
    }

    @Override
    public String driver()
    {
        return "org.elasticsearch.xpack.sql.jdbc.EsDriver";
    }
}
