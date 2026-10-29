package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class RedisService
        implements PluginService
{
    @Override
    public String connectType()
    {
        return "redis";
    }

    @Override
    public String driver()
    {
        return "org.devlive.metaforge.driver.RedisJdbcDriver";
    }
}
