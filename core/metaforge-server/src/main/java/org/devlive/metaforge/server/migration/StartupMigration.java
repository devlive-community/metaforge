package org.devlive.metaforge.server.migration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.server.configure.SetupDatasourceGuardEnvironmentPostProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Locale;

/**
 * Upgrades an installed metadata database before anything else touches it. Registered as a
 * database initializer, so every JDBC component waits for it. Fresh databases are left to the
 * setup wizard, and the temporary store used while the wizard runs is never migrated.
 */
@Slf4j
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class StartupMigration
        implements InitializingBean
{
    private final DataSource dataSource;
    private final Environment environment;

    public StartupMigration(DataSource dataSource, Environment environment)
    {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet()
            throws Exception
    {
        if (StringUtils.isBlank(environment.getProperty("spring.datasource.url"))
                || System.getProperty(SetupDatasourceGuardEnvironmentPostProcessor.FALLBACK_FROM_PROPERTY) != null) {
            log.info("Schema migration skipped: no configured metadata database");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            if (!hasMetadataTables(connection)) {
                log.info("Schema migration skipped: the metadata database is not installed yet");
                return;
            }
            DatabaseMigrator.migrate(connection);
            log.info("Metadata schema is up to date");
        }
    }

    static boolean hasMetadataTables(Connection connection)
            throws Exception
    {
        DatabaseMetaData metadata = connection.getMetaData();
        String table = metadata.storesUpperCaseIdentifiers() ? "METAFORGE_USER" : "metaforge_user";
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), connection.getSchema(), table, new String[] {"TABLE"})) {
            while (tables.next()) {
                if ("metaforge_user".equals(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }
}
