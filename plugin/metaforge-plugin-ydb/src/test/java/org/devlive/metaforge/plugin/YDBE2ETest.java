package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the YDB plugin through the YDB JDBC driver against every YDB release it documents as
 * verified: a table created with YQL is listed in the database with its columns, and its rows read
 * back.
 *
 * <p>The driver discovers the database's nodes and calls them by their host name, so the container
 * is named localhost and serves gRPC on the fixed port 2136.</p>
 */
@RunWith(Parameterized.class)
public class YDBE2ETest
{
    @Parameterized.Parameters(name = "YDB {0}")
    public static List<String> versions()
    {
        return List.of("25.4", "26.3");
    }

    private final String version;

    public YDBE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void tablesAndColumnsAreListedAndRead()
            throws Exception
    {
        try (GenericContainer<?> container = new FixedHostPortGenericContainer<>("ydbplatform/local-ydb:" + version)
                .withFixedExposedPort(2136, 2136)
                .withCreateContainerCmdModifier(command -> command.withHostName("localhost"))
                .withEnv("GRPC_PORT", "2136")
                .withEnv("YDB_USE_IN_MEMORY_PDISKS", "true")
                .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            PluginService service = new YDBService();
            RuntimeConfigure configure = configure("localhost", 2136, null, null, "local");

            // A fresh database can drop the first sessions it hands out; retry the check until one holds
            long deadline = System.currentTimeMillis() + Duration.ofMinutes(2).toMillis();
            Response checked = service.execute(configure, service.validator());
            while (!Boolean.TRUE.equals(checked.getIsSuccessful()) && System.currentTimeMillis() < deadline) {
                Thread.sleep(2000);
                checked = service.execute(configure, service.validator());
            }
            // Stable builds report their branch, e.g. stable-25-4-1; others their version, e.g. 26.3.1.4
            String reported = first(ok("validator", checked)).replace('-', '.');
            Assert.assertTrue(reported + " is not " + version, reported.contains(version + "."));

            ok("create table", service.execute(configure, "CREATE TABLE items (id Int32 NOT NULL, name Utf8, PRIMARY KEY (id))"));
            ok("insert", service.execute(configure, "UPSERT INTO items (id, name) VALUES (1, 'a'), (2, 'b')"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertFalse(databases.toString(), databases.isEmpty());
            List<String> tables = column(ok("tables", service.getTables(configure, databases.get(0))), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, databases.get(0), "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM items WHERE id = 2"))));

            ok("drop table", service.execute(configure, "DROP TABLE items"));
        }
    }
}
