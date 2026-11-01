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

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the Ignite plugin through the Ignite thin JDBC driver against every Apache Ignite 2 release
 * it documents as verified: a table created with SQL is listed in the PUBLIC schema with its columns,
 * and its rows read back.
 */
@RunWith(Parameterized.class)
public class IgniteE2ETest
{
    @Parameterized.Parameters(name = "Ignite {0}")
    public static List<String> versions()
    {
        return List.of("2.17.0", "2.18.0");
    }

    private final String version;

    public IgniteE2ETest(String version)
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
    {
        try (GenericContainer<?> container = new GenericContainer<>("apacheignite/ignite:" + version + "-arm64")
                .withExposedPorts(10800)
                .waitingFor(Wait.forLogMessage(".*Topology snapshot.*", 1).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new IgniteService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(10800), null, null, null);
            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version));

            ok("create table", service.execute(configure, "CREATE TABLE items (id INT PRIMARY KEY, name VARCHAR(32))"));
            ok("insert", service.execute(configure, "INSERT INTO items (id, name) VALUES (1, 'a'), (2, 'b')"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("PUBLIC"));
            List<String> tables = column(ok("tables", service.getTables(configure, "PUBLIC")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("ITEMS"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "PUBLIC", "ITEMS")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("ID", "NAME")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM items WHERE id = 2"))));

            ok("drop table", service.execute(configure, "DROP TABLE items"));
            Assert.assertFalse(column(ok("tables", service.getTables(configure, "PUBLIC")), "object_name").contains("ITEMS"));
        }
    }
}
