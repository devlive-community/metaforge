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
 * Runs the PostgreSQL plugin through its JDBC driver against every PostgreSQL release line it
 * documents as verified: schemas, tables and columns created with SQL are listed as databases,
 * tables and columns, and their rows read back.
 */
@RunWith(Parameterized.class)
public class PostgreSQLE2ETest
{
    @Parameterized.Parameters(name = "PostgreSQL {0}")
    public static List<String> versions()
    {
        return List.of("15", "16", "17");
    }

    private final String version;

    public PostgreSQLE2ETest(String version)
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
        try (GenericContainer<?> container = new GenericContainer<>("postgres:" + version)
                .withEnv("POSTGRES_PASSWORD", "metaforge")
                .withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new PostgreSQLService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(5432), "postgres", "metaforge", "postgres");

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith("PostgreSQL " + version + "."));

            ok("create schema", service.execute(configure, "CREATE SCHEMA e2e"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id INT PRIMARY KEY, name VARCHAR(32) NOT NULL)"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES (1, 'a'), (2, 'b')"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "e2e")), "object_name").contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM e2e.items WHERE id = 2"))));
            Assert.assertEquals("2", first(ok("count", service.execute(configure, "SELECT COUNT(*) AS total FROM e2e.items"))));

            ok("drop schema", service.execute(configure, "DROP SCHEMA e2e CASCADE"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
