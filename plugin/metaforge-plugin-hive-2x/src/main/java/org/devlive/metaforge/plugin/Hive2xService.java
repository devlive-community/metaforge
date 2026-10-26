package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class Hive2xService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "org.apache.hive.jdbc.HiveDriver";
    }

    @Override
    public String connectType()
    {
        return "hive2";
    }
}
