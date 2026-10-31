package org.devlive.metaforge.plugin.jdbc.cratedb;

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
 * Runs the CrateDB plugin through its JDBC driver (PostgreSQL protocol) against every CrateDB
 * release line it documents as verified. A CrateDB schema is the plugin's database: it comes
 * into being with its first table and goes with its last.
 */
@RunWith(Parameterized.class)
public class CrateDBE2ETest
{
    @Parameterized.Parameters(name = "CrateDB {0}")
    public static List<String> versions()
    {
        return List.of("5.10", "6.1");
    }

    private final String version;

    public CrateDBE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void schemasTablesAndColumnsAreManagedWithSql()
    {
        try (GenericContainer<?> container = new GenericContainer<>("crate:" + version)
                .withCommand("-Cdiscovery.type=single-node")
                .withExposedPorts(5432, 4200)
                .waitingFor(Wait.forHttp("/").forPort(4200).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new CrateDBService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(5432), "crate", null, null);

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.contains(version + "."));

            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id INTEGER PRIMARY KEY, name TEXT)"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "e2e")), "object_name").contains("items"));

            ok("insert", service.execute(configure, "INSERT INTO e2e.items (id, name) VALUES (1, 'a'), (2, 'b')"));
            ok("refresh", service.execute(configure, "REFRESH TABLE e2e.items"));
            Assert.assertEquals("2", first(ok("count", service.execute(configure, "SELECT count(*) AS total FROM e2e.items"))));

            ok("add column", service.execute(configure, "ALTER TABLE e2e.items ADD COLUMN created TIMESTAMP WITH TIME ZONE"));
            ok("rename column", service.execute(configure, "ALTER TABLE e2e.items RENAME COLUMN name TO title"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "title", "created")));
            ok("drop column", service.execute(configure, "ALTER TABLE e2e.items DROP COLUMN created"));
            Assert.assertFalse(column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name").contains("created"));

            ok("drop table", service.execute(configure, "DROP TABLE e2e.items"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
