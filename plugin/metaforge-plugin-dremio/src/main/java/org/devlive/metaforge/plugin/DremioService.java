package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.connection.JdbcUrlGuard;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.ArrayList;
import java.util.List;

/**
 * Dremio through the Arrow Flight SQL JDBC driver on its Flight port (32010). Dremio's legacy JDBC
 * driver bundles a Netty that does not run on Java 21.
 */
@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class DremioService
        implements PluginService
{
    @Override
    public String validator()
    {
        return "SELECT version FROM sys.version";
    }

    @Override
    public String driver()
    {
        return "org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver";
    }

    @Override
    public String connectType()
    {
        return "arrow-flight-sql";
    }

    @Override
    public String url(RuntimeConfigure configure)
    {
        if (configure.getUrl().isPresent()) {
            return configure.getUrl().get();
        }
        List<String> parameters = new ArrayList<>();
        parameters.add("useEncryption=" + configure.getSsl().orElse(false));
        configure.getEnv().ifPresent(env -> env.forEach((key, value) -> {
            JdbcUrlGuard.validateParameter(key, String.valueOf(value));
            parameters.add(key + "=" + value);
        }));
        return "jdbc:" + configure.getType() + "://" + JdbcUrlGuard.validateHost(configure.getHost()) + ":" + configure.getPort()
                + "?" + String.join("&", parameters);
    }
}
