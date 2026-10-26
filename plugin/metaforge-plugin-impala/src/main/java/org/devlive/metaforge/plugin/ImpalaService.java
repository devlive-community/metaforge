package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;

public class ImpalaService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "impala";
    }

    @Override
    public String driver()
    {
        return "com.cloudera.impala.jdbc.Driver";
    }
}
