package org.devlive.metaforge.plugin.dolphindb;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
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

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.assumeImageSupportsHost;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the DolphinDB plugin through the DolphinDB JDBC driver against every DolphinDB release it
 * documents as verified: a distributed database and table created with SQL are listed as a database,
 * table and columns, and their rows read back.
 */
@RunWith(Parameterized.class)
public class DolphinDBE2ETest
{
    @Parameterized.Parameters(name = "DolphinDB {0}")
    public static List<String> versions()
    {
        return List.of("v2.00.18", "v3.00.6");
    }

    private final String version;

    public DolphinDBE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void databasesTablesAndColumnsAreListedAndRead() throws Exception
    {
        String image = "dolphindb/dolphindb:" + version;
        assumeImageSupportsHost(image);
        try (GenericContainer<?> container = new GenericContainer<>(image)
                .withExposedPorts(8848)
                .waitingFor(Wait.forHttp("/").forPort(8848).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new DolphinDBService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(8848), "admin", "123456", null);
            // The web port answers before the public key is initialized; the retrying ok()
            // repeats the login until the node accepts it
            String reported = first(ok("validator", () -> service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version.substring(1)));

            // The web port answers before the data node takes writes; retry creating the database until it does
            String createDatabase = "create database \"dfs://e2e\" partitioned by VALUE(1..10)";
            long deadline = System.currentTimeMillis() + Duration.ofMinutes(1).toMillis();
            Response created = service.execute(configure, createDatabase);
            while (!Boolean.TRUE.equals(created.getIsSuccessful()) && System.currentTimeMillis() < deadline) {
                Thread.sleep(1000);
                created = service.execute(configure, createDatabase);
            }
            ok("create database", created);
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("dfs://e2e"));
            ok("create table", service.execute(configure, "create table \"dfs://e2e\".\"items\"(id INT, name STRING) partitioned by id"));
            // The driver rewrites INSERT INTO a loadTable() call into a script the server rejects; append with a script
            ok("append", service.execute(configure, "loadTable(\"dfs://e2e\", \"items\").append!(table([1, 2] as id, [\"a\", \"b\"] as name))"));
            List<String> tables = column(ok("tables", service.getTables(configure, "dfs://e2e")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "dfs://e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "select name from loadTable(\"dfs://e2e\", \"items\") where id = 2"))));
        }
    }
}
