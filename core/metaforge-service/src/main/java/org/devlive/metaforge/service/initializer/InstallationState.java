package org.devlive.metaforge.service.initializer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * Shared first-boot guard for setup and startup tasks. Check metadata before
 * querying so a fresh database does not produce missing-table SQL errors.
 */
@Component
public class InstallationState
{
    private final DataSource dataSource;

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "DataSource is an injected Spring-managed singleton")
    public InstallationState(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public boolean isInstalled()
    {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String table = metadata.storesUpperCaseIdentifiers() ? "METAFORGE_USER" : "metaforge_user";
            boolean exists = false;
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), connection.getSchema(), table, new String[] {"TABLE"})) {
                while (tables.next()) {
                    if ("metaforge_user".equals(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT))) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                return false;
            }
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM metaforge_user")) {
                return result.next() && result.getLong(1) > 0;
            }
        }
        catch (SQLException e) {
            return false;
        }
    }
}
