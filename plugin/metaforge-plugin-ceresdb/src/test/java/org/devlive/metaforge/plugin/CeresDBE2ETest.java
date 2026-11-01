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
 * Runs the CeresDB plugin through the PostgreSQL JDBC driver against every CeresDB (HoraeDB)
 * release it documents as verified. CeresDB has one schema, public: tables are created, read and dropped,
 * and columns added, with SQL.
 */
@RunWith(Parameterized.class)
public class CeresDBE2ETest
{
    /** CeresDB 2 is published as Apache HoraeDB; its image is amd64-only. */
    @Parameterized.Parameters(name = "{0}")
    public static List<String> images()
    {
        return List.of("ceresdb/ceresdb-server:v1.2.7", "apache/horaedb-server:v2.1.0");
    }

    private final String image;

    public CeresDBE2ETest(String image)
    {
        this.image = image;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void tablesAndColumnsAreManagedWithSql()
    {
        try (GenericContainer<?> container = new GenericContainer<>(image)
                .withExposedPorts(5433, 5440)
                .waitingFor(Wait.forListeningPorts(5433).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new CeresDBService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(5433), null, null, null);

            Assert.assertEquals("1", first(ok("validator", service.execute(configure, service.validator()))));
            Assert.assertEquals(List.of("public"), column(ok("databases", service.getDatabases(configure)), "object_name"));

            ok("create table", service.execute(configure, "CREATE TABLE items (name string TAG, ts timestamp NOT NULL, cpu double, TIMESTAMP KEY(ts)) ENGINE=Analytic"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "public")), "object_name").contains("items"));
            ok("insert", service.execute(configure, "INSERT INTO items (name, ts, cpu) VALUES ('a', " + System.currentTimeMillis() + ", 0.5)"));
            Assert.assertEquals("1", first(ok("count", service.execute(configure, "SELECT count(*) AS total FROM items"))));

            ok("add column", service.execute(configure, "ALTER TABLE items ADD COLUMN mem double"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "public", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("name", "ts", "cpu", "mem")));

            ok("drop table", service.execute(configure, "DROP TABLE items"));
            Assert.assertFalse(column(ok("tables", service.getTables(configure, "public")), "object_name").contains("items"));
        }
    }
}
