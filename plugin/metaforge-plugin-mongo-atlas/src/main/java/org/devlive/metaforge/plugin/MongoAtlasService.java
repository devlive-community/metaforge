package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class MongoAtlasService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "mongodb";
    }

    @Override
    public String driver()
    {
        return "com.mongodb.jdbc.MongoDriver";
    }
}
