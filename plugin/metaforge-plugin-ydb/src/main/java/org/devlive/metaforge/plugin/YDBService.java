package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class YDBService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "ydb";
    }

    @Override
    public String driver()
    {
        return "com.yandex.ydb.jdbc.YdbDriver";
    }
}
