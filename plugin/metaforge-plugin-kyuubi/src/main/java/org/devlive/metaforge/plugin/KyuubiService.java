package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class KyuubiService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "kyuubi";
    }

    @Override
    public String driver()
    {
        return "org.apache.kyuubi.jdbc.KyuubiHiveDriver";
    }
}
