package org.devlive.metaforge.service.initializer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class RoleCodeConstraint
{
    private final DataSource dataSource;

    public RoleCodeConstraint(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    /** Upgrade legacy stores lazily; never query an uninstalled database at startup. */
    public synchronized void ensure()
            throws SQLException
    {
        try (Connection connection = dataSource.getConnection()) {
            if (hasUniqueCodeIndex(connection)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE UNIQUE INDEX uk_metaforge_role_code ON metaforge_role (code)");
            }
            catch (SQLException exception) {
                // Another instance may have installed the same constraint concurrently.
                if (!hasUniqueCodeIndex(connection)) {
                    throw exception;
                }
            }
        }
    }

    private boolean hasUniqueCodeIndex(Connection connection)
            throws SQLException
    {
        DatabaseMetaData metadata = connection.getMetaData();
        String table = metadata.storesUpperCaseIdentifiers() ? "METAFORGE_ROLE" : "metaforge_role";
        Map<String, List<String>> indexes = new HashMap<>();
        try (ResultSet rows = metadata.getIndexInfo(connection.getCatalog(), null, table, true, false)) {
            while (rows.next()) {
                String index = rows.getString("INDEX_NAME");
                String column = rows.getString("COLUMN_NAME");
                if (index != null && column != null) {
                    indexes.computeIfAbsent(index, key -> new ArrayList<>()).add(column.toLowerCase(Locale.ROOT));
                }
            }
        }
        return indexes.values().stream().anyMatch(columns -> columns.size() == 1 && columns.contains("code"));
    }
}
