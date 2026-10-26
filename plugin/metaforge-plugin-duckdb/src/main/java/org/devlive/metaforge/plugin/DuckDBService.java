package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class DuckDBService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "org.duckdb.DuckDBDriver";
    }

    @Override
    public String connectType()
    {
        return "duckdb";
    }
}
