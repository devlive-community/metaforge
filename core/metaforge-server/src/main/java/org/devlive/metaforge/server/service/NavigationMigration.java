package org.devlive.metaforge.server.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** One-time catalogue upgrade; callers supply a dedicated connection. */
public final class NavigationMigration
{
    private static final String KEY = "navigation-user-admin-v1";

    private NavigationMigration()
    {
    }

    public static void apply(Connection connection)
            throws Exception
    {
        boolean autoCommit = connection.getAutoCommit();
        try {
            // MySQL DDL commits implicitly, so prepare the marker before catalogue DML.
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS metaforge_schema_migration (migration_key VARCHAR(100) PRIMARY KEY, applied_at TIMESTAMP NULL DEFAULT NULL)");
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT IGNORE INTO metaforge_schema_migration (migration_key) VALUES (?)")) {
                insert.setString(1, KEY);
                insert.executeUpdate();
            }
            connection.setAutoCommit(false);
            try {
                boolean applied;
                try (PreparedStatement lock = connection.prepareStatement("SELECT applied_at FROM metaforge_schema_migration WHERE migration_key = ? FOR UPDATE")) {
                    lock.setString(1, KEY);
                    try (ResultSet state = lock.executeQuery()) {
                        if (!state.next()) {
                            throw new SQLException("Navigation migration state is missing");
                        }
                        applied = state.getTimestamp(1) != null;
                    }
                }
                if (!applied) {
                    ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/navigation-migration.sql"));
                    try (PreparedStatement update = connection.prepareStatement("UPDATE metaforge_schema_migration SET applied_at = CURRENT_TIMESTAMP WHERE migration_key = ?")) {
                        update.setString(1, KEY);
                        update.executeUpdate();
                    }
                }
                connection.commit();
            }
            catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
        finally {
            connection.setAutoCommit(autoCommit);
        }
    }
}
