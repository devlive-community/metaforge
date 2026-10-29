package org.devlive.metaforge.server.migration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.server.support.TestDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applies the changelog to the database selected with {@code -Dmetaforge.it.database}
 * (fresh install) and upgrades a pre-changelog installation, which only ever existed on H2 and MySQL.
 */
@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "COUNT(*) and selected fixture rows are never null")
public class DatabaseMigratorTest
{
    private static TestDatabase database;

    @BeforeAll
    public static void start()
    {
        database = TestDatabase.start(TestDatabase.selectedSpec());
    }

    @AfterAll
    public static void stop()
    {
        database.close();
    }

    @Test
    public void freshInstallCreatesSchemaAndSeedAndIsRepeatable()
            throws Exception
    {
        try (Connection connection = database.connect()) {
            DatabaseMigrator.migrate(connection);
            DatabaseMigrator.migrate(connection);

            JdbcTemplate jdbc = jdbc(connection);
            assertEquals(20, count(jdbc, "SELECT COUNT(*) FROM metaforge_menu"));
            assertEquals(4, count(jdbc, "SELECT COUNT(*) FROM metaforge_role"));
            assertEquals(28, count(jdbc, "SELECT COUNT(*) FROM metaforge_role_menu"));
            assertEquals(2, count(jdbc, "SELECT COUNT(*) FROM metaforge_function"));
            assertEquals(2, count(jdbc, "SELECT COUNT(*) FROM metaforge_scheduled"));
            assertEquals("系统管理员", jdbc.queryForObject("SELECT name FROM metaforge_role WHERE code = 'ADMIN'", String.class));
            assertEquals(count(jdbc, "SELECT id FROM metaforge_menu WHERE code = 'MANAGEMENT'"),
                    count(jdbc, "SELECT parent FROM metaforge_menu WHERE code = 'QUERY'"));
            assertEquals(18, count(jdbc, "SELECT COUNT(*) FROM metaforge_role_menu rm JOIN metaforge_role r ON r.id = rm.role_id WHERE r.code = 'ADMIN'"));

            // Unicode text, booleans, timestamps and generated keys behave the same everywhere.
            jdbc.update("INSERT INTO metaforge_source (name, code, source_type, host, port, is_public, use_ssl, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    "权限-ÄÖÜ", "src-1", "MySQL", "localhost", 3306, true, false);
            assertEquals("权限-ÄÖÜ", jdbc.queryForObject("SELECT name FROM metaforge_source WHERE code = 'src-1'", String.class));
            assertTrue(jdbc.queryForObject("SELECT is_public FROM metaforge_source WHERE code = 'src-1'", Boolean.class));
        }
    }

    @Test
    public void legacyH2InstallationUpgradesToTheFreshSchema()
            throws Exception
    {
        try (TestDatabase legacy = TestDatabase.h2(); TestDatabase fresh = TestDatabase.h2()) {
            assertLegacyUpgrade(legacy, fresh, "legacy/setup-schema-h2.sql");
        }
    }

    @Test
    public void legacyMysqlInstallationUpgradesToTheFreshSchema()
            throws Exception
    {
        if (!database.engine().equals("mysql")) {
            return;
        }
        String base = database.url().substring(0, database.url().lastIndexOf('/') + 1);
        try (Connection connection = DriverManager.getConnection(base, "root", database.password());
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE legacy_upgrade");
            statement.execute("CREATE DATABASE legacy_fresh");
        }
        try (Connection legacy = DriverManager.getConnection(base + "legacy_upgrade", "root", database.password());
                Connection fresh = DriverManager.getConnection(base + "legacy_fresh", "root", database.password())) {
            assertLegacyUpgrade(legacy, fresh, "legacy/setup-schema.sql");
        }
    }

    private void assertLegacyUpgrade(TestDatabase legacy, TestDatabase fresh, String schema)
            throws Exception
    {
        try (Connection legacyConnection = legacy.connect(); Connection freshConnection = fresh.connect()) {
            assertLegacyUpgrade(legacyConnection, freshConnection, schema);
        }
    }

    private void assertLegacyUpgrade(Connection legacy, Connection fresh, String schema)
            throws Exception
    {
        ScriptUtils.executeSqlScript(legacy, new ClassPathResource(schema));
        ScriptUtils.executeSqlScript(legacy, new ClassPathResource("legacy/setup-data.sql"));
        JdbcTemplate jdbc = jdbc(legacy);
        // Business rows linked the way the JPA mapping wrote them.
        jdbc.update("INSERT INTO metaforge_user (id, username, code) VALUES (7, 'alice', 'u7')");
        jdbc.update("INSERT INTO metaforge_user_role_relation (user_id, role_id) VALUES (7, 1), (7, 1), (7, 999)");
        jdbc.update("INSERT INTO metaforge_source (id, name, host, port, `_type`, `public`, user_id, code) VALUES (11, 'warehouse', 'db', 3306, 'Redis Jdbc', 1, 7, 's11')");
        jdbc.update("INSERT INTO metaforge_dataset (id, name, code) VALUES (21, 'orders', 'd21')");
        jdbc.update("INSERT INTO metaforge_dataset_user_relation (dataset_id, user_id) VALUES (21, 7)");
        jdbc.update("INSERT INTO metaforge_dataset_source_relation (dataset_id, source_id) VALUES (21, 11)");
        jdbc.update("INSERT INTO metaforge_dataset_column (id, name, `comment`, is_primary_key, code) VALUES (31, 'id', 'key', '1', 'c31')");
        jdbc.update("INSERT INTO metaforge_dataset_column_relation (dataset_id, column_id) VALUES (21, 31)");
        jdbc.update("INSERT INTO metaforge_dataset_history (id, name, code) VALUES (41, 'run', 'h41')");
        jdbc.update("INSERT INTO metaforge_dataset_history_relation (dataset_history_id, dataset_id) VALUES (41, 21)");
        jdbc.update("INSERT INTO metaforge_report (id, name, code) VALUES (51, 'sales', 'r51')");
        jdbc.update("INSERT INTO metaforge_report_user_relation (report_id, user_id) VALUES (51, 7)");
        jdbc.update("INSERT INTO metaforge_report_dataset_relation (report_id, dataset_id) VALUES (51, 21)");
        jdbc.update("INSERT INTO metaforge_dashboard (id, name, code) VALUES (61, 'board', 'b61')");
        jdbc.update("INSERT INTO metaforge_dashboard_user_relation (dashboard_id, user_id) VALUES (61, 7)");
        jdbc.update("INSERT INTO metaforge_dashboard_report_relation (dashboard_id, report_id) VALUES (61, 51)");
        jdbc.update("INSERT INTO metaforge_chat (id, name, code) VALUES (71, 'talk', 'ch71')");
        jdbc.update("INSERT INTO metaforge_chat_user_relation (chat_id, user_id) VALUES (71, 7)");
        jdbc.update("INSERT INTO metaforge_message (id, name, code) VALUES (81, 'hi', 'm81')");
        jdbc.update("INSERT INTO metaforge_message_chat_relation (message_id, chat_id) VALUES (81, 71)");
        jdbc.update("INSERT INTO metaforge_message_user_relation (message_id, user_id) VALUES (81, 7)");
        jdbc.update("INSERT INTO metaforge_scheduled_history (id, name, code) VALUES (91, 'tick', 'sh91')");
        // history-owned join table: scheduled_id = history id, history_id = schedule id
        jdbc.update("INSERT INTO metaforge_scheduled_history_relation (scheduled_id, history_id) VALUES (91, 2)");
        jdbc.update("INSERT INTO metaforge_scheduled_history_source_relation (scheduled_history_id, source_id) VALUES (91, 11)");
        jdbc.update("INSERT INTO metaforge_workflow (id, name, j_from_id, j_to_id, j_user_id, code) VALUES (101, 'flow', 11, 11, 7, 'w101')");
        jdbc.update("INSERT INTO metaforge_source_query (id, plugin_id, user_id, content, create_time) VALUES (111, 11, 7, 'select 1', '2026-10-28 10:11:12.345')");

        DatabaseMigrator.migrate(legacy);
        DatabaseMigrator.migrate(legacy);
        DatabaseMigrator.migrate(fresh);

        assertEquals(columns(fresh), columns(legacy));
        assertFalse(columns(legacy).keySet().stream().anyMatch(table -> table.endsWith("_relation")));

        assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM metaforge_user_role WHERE user_id = 7 AND role_id = 1"));
        assertEquals(0, count(jdbc, "SELECT COUNT(*) FROM metaforge_user_role WHERE role_id = 999"));
        assertEquals(18, count(jdbc, "SELECT COUNT(*) FROM metaforge_role_menu WHERE role_id = 1"));
        assertEquals("Redis", jdbc.queryForObject("SELECT source_type FROM metaforge_source WHERE id = 11", String.class));
        assertTrue(jdbc.queryForObject("SELECT is_public FROM metaforge_source WHERE id = 11", Boolean.class));
        assertEquals(7, count(jdbc, "SELECT user_id FROM metaforge_dataset WHERE id = 21"));
        assertEquals(11, count(jdbc, "SELECT source_id FROM metaforge_dataset WHERE id = 21"));
        assertEquals(21, count(jdbc, "SELECT dataset_id FROM metaforge_dataset_column WHERE id = 31"));
        assertTrue(jdbc.queryForObject("SELECT is_primary_key FROM metaforge_dataset_column WHERE id = 31", Boolean.class));
        assertEquals("key", jdbc.queryForObject("SELECT column_comment FROM metaforge_dataset_column WHERE id = 31", String.class));
        assertEquals(21, count(jdbc, "SELECT dataset_id FROM metaforge_dataset_history WHERE id = 41"));
        assertEquals(7, count(jdbc, "SELECT user_id FROM metaforge_report WHERE id = 51"));
        assertEquals(21, count(jdbc, "SELECT dataset_id FROM metaforge_report WHERE id = 51"));
        assertEquals(7, count(jdbc, "SELECT user_id FROM metaforge_dashboard WHERE id = 61"));
        assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM metaforge_dashboard_report WHERE dashboard_id = 61 AND report_id = 51"));
        assertEquals(7, count(jdbc, "SELECT user_id FROM metaforge_chat WHERE id = 71"));
        assertEquals(71, count(jdbc, "SELECT chat_id FROM metaforge_message WHERE id = 81"));
        assertEquals(7, count(jdbc, "SELECT user_id FROM metaforge_message WHERE id = 81"));
        assertEquals(2, count(jdbc, "SELECT scheduled_id FROM metaforge_scheduled_history WHERE id = 91"));
        assertEquals(11, count(jdbc, "SELECT source_id FROM metaforge_scheduled_history WHERE id = 91"));
        assertEquals(11, count(jdbc, "SELECT from_source_id FROM metaforge_workflow WHERE id = 101"));
        assertEquals(7, count(jdbc, "SELECT user_id FROM metaforge_workflow WHERE id = 101"));
        assertEquals(11, count(jdbc, "SELECT source_id FROM metaforge_source_query WHERE id = 111"));
        assertEquals("2026-10-28 10:11:12", jdbc.queryForObject("SELECT create_time FROM metaforge_source_query WHERE id = 111", java.sql.Timestamp.class)
                .toLocalDateTime().toString().replace('T', ' '));
        // the navigation catalogue upgrade ran, and the seed did not duplicate the legacy catalogue
        assertEquals("/workspace", jdbc.queryForObject("SELECT url FROM metaforge_menu WHERE code = 'MANAGEMENT'", String.class));
        assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM metaforge_menu WHERE code = 'HOME'"));
    }

    /** Table name to column names, lower-cased, metadata tables only. */
    private static Map<String, Set<String>> columns(Connection connection)
            throws SQLException
    {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        Map<String, Set<String>> tables = new TreeMap<>();
        try (ResultSet result = meta.getColumns(catalog, schema, null, null)) {
            while (result.next()) {
                String table = result.getString("TABLE_NAME").toLowerCase(Locale.ROOT);
                if (table.startsWith("metaforge_") && !table.startsWith(DatabaseMigrator.CHANGELOG_TABLE)) {
                    tables.computeIfAbsent(table, key -> new TreeSet<>()).add(result.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
        }
        return tables;
    }

    private static JdbcTemplate jdbc(Connection connection)
    {
        return new JdbcTemplate(new SingleConnectionDataSource(connection, true));
    }

    private static long count(JdbcTemplate jdbc, String sql)
    {
        return jdbc.queryForObject(sql, Long.class);
    }
}
