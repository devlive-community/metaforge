package org.devlive.metaforge.plugin.jdbc.cratedb;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;

@Slf4j
public class CrateDBService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "crate";
    }

    @Override
    public String driver()
    {
        return "io.crate.client.jdbc.CrateDriver";
    }
}
