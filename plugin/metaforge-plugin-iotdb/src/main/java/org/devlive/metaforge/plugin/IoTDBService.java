package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class IoTDBService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "org.apache.iotdb.jdbc.IoTDBDriver";
    }

    @Override
    public String connectType()
    {
        return "iotdb";
    }
}
