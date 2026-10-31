package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.Set;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class RedisService
        implements PluginService
{
    @Override
    public ManagementCapabilities managementCapabilities()
    {
        return new ManagementCapabilities("key-value", Set.of("namespace:list", "key:list", "key:read", "tab:keys", "tab:sql", "query:execute"));
    }

    @Override
    public ManagementPage managementNamespaces(RuntimeConfigure configure, ManagementRequest request)
    {
        return new RedisManagement().namespaces(configure);
    }

    @Override
    public ManagementPage managementList(RuntimeConfigure configure, ManagementRequest request)
    {
        return new RedisManagement().list(configure, request);
    }

    @Override
    public ManagementDetail managementDetail(RuntimeConfigure configure, ManagementRequest request)
    {
        return new RedisManagement().detail(configure, request);
    }

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
