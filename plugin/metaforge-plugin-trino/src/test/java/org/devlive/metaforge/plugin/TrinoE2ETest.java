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
 * Runs the Trino plugin through the Trino JDBC driver against every Trino release it documents as
 * verified: a schema and table created with SQL in the memory catalog are listed as a database,
 * table and columns, and their rows read back.
 */
@RunWith(Parameterized.class)
public class TrinoE2ETest
{
    @Parameterized.Parameters(name = "Trino {0}")
    public static List<String> versions()
    {
        return List.of("470", "476");
    }

    private final String version;

    public TrinoE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void schemasTablesAndColumnsAreListedAndRead()
    {
        try (GenericContainer<?> container = new GenericContainer<>("trinodb/trino:" + version)
                .withExposedPorts(8080)
                .waitingFor(Wait.forLogMessage(".*SERVER STARTED.*", 1).withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            PluginService service = new TrinoService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(8080), "metaforge", null, "memory");
            Assert.assertEquals(version, first(ok("validator", () -> service.execute(configure, service.validator()))));

            ok("create schema", () -> service.execute(configure, "CREATE SCHEMA memory.e2e"));
            List<String> databases = column(ok("databases", () -> service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("e2e"));
            ok("create table", () -> service.execute(configure, "CREATE TABLE memory.e2e.items (id INTEGER, name VARCHAR)"));
            ok("insert", () -> service.execute(configure, "INSERT INTO memory.e2e.items VALUES (1, 'a'), (2, 'b')"));
            List<String> tables = column(ok("tables", () -> service.getTables(configure, "e2e")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("items"));
            List<String> columns = column(ok("columns", () -> service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
            Assert.assertEquals("b", first(ok("select", () -> service.execute(configure, "SELECT name FROM memory.e2e.items WHERE id = 2"))));

            ok("drop table", () -> service.execute(configure, "DROP TABLE memory.e2e.items"));
            ok("drop schema", () -> service.execute(configure, "DROP SCHEMA memory.e2e"));
            Assert.assertFalse(column(ok("databases", () -> service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
