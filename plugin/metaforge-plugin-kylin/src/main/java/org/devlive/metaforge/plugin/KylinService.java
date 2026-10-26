package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class KylinService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "kylin";
    }

    @Override
    public String driver()
    {
        return "org.apache.kylin.jdbc.Driver";
    }
}
