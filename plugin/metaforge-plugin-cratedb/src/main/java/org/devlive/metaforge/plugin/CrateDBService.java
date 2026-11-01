package org.devlive.metaforge.plugin;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

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

    /** The driver refuses a URL without a slash after the port, which a source without a schema produces. */
    @Override
    public String url(RuntimeConfigure configure)
    {
        String url = PluginService.super.url(configure);
        if (configure.getUrl().isPresent() || configure.getDatabase().isPresent()) {
            return url;
        }
        return url.replaceFirst("^(jdbc:crate://[^/?]+)(\\?|$)", "$1/$2");
    }
}
