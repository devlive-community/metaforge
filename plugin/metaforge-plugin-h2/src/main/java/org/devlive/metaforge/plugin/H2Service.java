package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.connection.JdbcUrlGuard;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class H2Service
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
    public String connectType()
    {
        return "h2:mem";
    }

    @Override
    public String url(RuntimeConfigure configure)
    {
        if (configure.getUrl().isPresent()) {
            return configure.getUrl().get();
        }
        String database = configure.getDatabase().orElse("metaforge");
        JdbcUrlGuard.validateDatabase(database);
        // Each query opens and closes its own connection; keep the in-memory database once the last one closes
        return String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1", database);
    }

    @Override
    public String driver()
    {
        return "org.h2.Driver";
    }

}
