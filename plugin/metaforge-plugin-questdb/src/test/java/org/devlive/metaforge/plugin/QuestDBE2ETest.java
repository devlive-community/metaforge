package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the QuestDB plugin through the PostgreSQL JDBC driver against every QuestDB release line
 * it documents as verified. A QuestDB server is one database (qdb); tables and columns are
 * created, read, changed and dropped with SQL.
 */
@RunWith(Parameterized.class)
public class QuestDBE2ETest
{
    @Parameterized.Parameters(name = "QuestDB {0}")
    public static List<String> versions()
    {
        return List.of("8.3.3", "9.1.0");
    }

    private final String version;

    public QuestDBE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void tablesAndColumnsAreManagedWithSql()
    {
        try (GenericContainer<?> container = new GenericContainer<>("questdb/questdb:" + version)
                .withExposedPorts(8812, 9000)
                .waitingFor(Wait.forHttp("/").forPort(9000).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new QuestDBService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(8812), "admin", "quest", null);

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.contains("QuestDB " + version));
            Assert.assertEquals(List.of("qdb"), column(ok("databases", service.getDatabases(configure)), "object_name"));

            ok("create table", service.execute(configure, "CREATE TABLE items (id INT, name STRING, ts TIMESTAMP) TIMESTAMP(ts) PARTITION BY DAY WAL"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "qdb")), "object_name").contains("items"));
            ok("insert", service.execute(configure, "INSERT INTO items VALUES (1, 'a', now()), (2, 'b', now())"));

            ok("add column", service.execute(configure, "ALTER TABLE items ADD COLUMN created TIMESTAMP"));
            ok("rename column", service.execute(configure, "ALTER TABLE items RENAME COLUMN name TO title"));
            Assert.assertTrue("columns are not changed", eventually(() -> column(ok("columns", service.getColumns(configure, "qdb", "items")), "object_name")
                    .containsAll(List.of("id", "title", "ts", "created"))));
            ok("drop column", service.execute(configure, "ALTER TABLE items DROP COLUMN created"));
            // WAL tables apply schema changes in the background.
            Assert.assertTrue("created is still listed", eventually(() -> !column(ok("columns", service.getColumns(configure, "qdb", "items")), "object_name").contains("created")));

            ok("drop table", service.execute(configure, "DROP TABLE items"));
            Assert.assertFalse(column(ok("tables", service.getTables(configure, "qdb")), "object_name").contains("items"));
        }
    }

    private static boolean eventually(BooleanSupplier condition)
    {
        for (int attempt = 0; attempt < 50; attempt++) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
