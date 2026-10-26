package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class MySQLService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "mysql";
    }

    @Override
    public String driver()
    {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public Boolean isSupportMeta()
    {
        return true;
    }
}
