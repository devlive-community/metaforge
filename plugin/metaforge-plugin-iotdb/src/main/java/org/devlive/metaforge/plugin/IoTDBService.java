package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class IoTDBService
        implements PluginService
{
    @Override
    public String validator()
    {
        // IoTDB has no version() function; SHOW VERSION starts with the version
        return "SHOW VERSION";
    }

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
