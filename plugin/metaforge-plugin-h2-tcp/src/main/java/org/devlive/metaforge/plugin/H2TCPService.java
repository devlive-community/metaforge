package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class H2TCPService
        implements PluginService
{
    @Override
    public String validator()
    {
        return "SELECT SETTING_VALUE AS version\n" +
                "FROM INFORMATION_SCHEMA.SETTINGS\n" +
                "WHERE SETTING_NAME  = 'info.VERSION'";
    }

    @Override
    public String driver()
    {
        return "org.h2.Driver";
    }

    @Override
    public String connectType()
    {
        return "h2:tcp";
    }
}
