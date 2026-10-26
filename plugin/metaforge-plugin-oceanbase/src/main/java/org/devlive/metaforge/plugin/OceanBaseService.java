package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class OceanBaseService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "com.oceanbase.jdbc.Driver";
    }

    @Override
    public String connectType()
    {
        return "oceanbase";
    }
}
