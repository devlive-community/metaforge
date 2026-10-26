package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class TDengineService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "com.taosdata.jdbc.TSDBDriver";
    }

    @Override
    public String connectType()
    {
        return "TAOS-RS";
    }
}
