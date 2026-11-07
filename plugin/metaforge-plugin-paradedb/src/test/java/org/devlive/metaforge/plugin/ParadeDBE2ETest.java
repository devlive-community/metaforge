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
 * Runs the ParadeDB plugin through the PostgreSQL JDBC driver against every ParadeDB release it
 * documents as verified: a table with a BM25 index created with SQL is listed as a table with its
 * columns, and searched.
 */
@RunWith(Parameterized.class)
public class ParadeDBE2ETest
{
    @Parameterized.Parameters(name = "ParadeDB {0}")
    public static List<String> versions()
    {
        return List.of("0.15.26-pg16", "0.19.5-pg17");
    }

    private final String version;

    public ParadeDBE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void searchIndexedTablesAreListedAndRead()
    {
        try (GenericContainer<?> container = new GenericContainer<>("paradedb/paradedb:" + version)
                .withEnv("POSTGRES_PASSWORD", "metaforge")
                .withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new ParadeDBService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(5432), "postgres", "metaforge", "postgres");
            String extension = version.substring(0, version.indexOf('-'));
            String postgres = version.substring(version.indexOf("-pg") + 3);
            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not PostgreSQL " + postgres, reported.startsWith("PostgreSQL " + postgres + "."));
            ok("extension", service.execute(configure, "CREATE EXTENSION IF NOT EXISTS pg_search"));
            Assert.assertEquals(extension, first(ok("extension version", service.execute(configure, "SELECT extversion FROM pg_extension WHERE extname = 'pg_search'"))));

            ok("create schema", service.execute(configure, "CREATE SCHEMA e2e"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id INT PRIMARY KEY, description TEXT)"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES (1, 'red running shoes'), (2, 'blue rain jacket')"));
            ok("bm25 index", service.execute(configure, "CREATE INDEX items_search ON e2e.items USING bm25 (id, description) WITH (key_field = 'id')"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "e2e")), "object_name").contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "description")));
            Assert.assertEquals("2", first(ok("search", service.execute(configure, "SELECT id FROM e2e.items WHERE description @@@ 'jacket'"))));

            ok("drop schema", service.execute(configure, "DROP SCHEMA e2e CASCADE"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
