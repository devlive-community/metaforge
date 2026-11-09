package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class ElasticSearch8xService
        implements PluginService
{
    @Override
    public String validator()
    {
        // Elasticsearch SQL has no version() function and no other way to read the server version
        return "SELECT '-' AS version";
    }

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
