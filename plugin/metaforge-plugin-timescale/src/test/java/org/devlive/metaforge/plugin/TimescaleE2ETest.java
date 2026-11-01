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
 * Runs the Timescale plugin through the PostgreSQL JDBC driver against every TimescaleDB release
 * it documents as verified: a hypertable created with SQL is listed as a table with its columns, and
 * its rows read back.
 */
@RunWith(Parameterized.class)
public class TimescaleE2ETest
{
    @Parameterized.Parameters(name = "TimescaleDB {0}")
    public static List<String> versions()
    {
        return List.of("2.17.2-pg16", "2.22.1-pg17");
    }

    private final String version;

    public TimescaleE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void hypertablesAreListedAndRead()
    {
        try (GenericContainer<?> container = new GenericContainer<>("timescale/timescaledb:" + version)
                .withEnv("POSTGRES_PASSWORD", "metaforge")
                .withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new TimescaleService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(5432), "postgres", "metaforge", "postgres");
            String extension = version.substring(0, version.indexOf('-'));
            String postgres = version.substring(version.indexOf("-pg") + 3);
            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not PostgreSQL " + postgres, reported.startsWith("PostgreSQL " + postgres + "."));
            Assert.assertEquals(extension, first(ok("extension", service.execute(configure, "SELECT extversion FROM pg_extension WHERE extname = 'timescaledb'"))));

            ok("create schema", service.execute(configure, "CREATE SCHEMA e2e"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.metrics (time TIMESTAMPTZ NOT NULL, device TEXT, reading DOUBLE PRECISION)"));
            ok("hypertable", service.execute(configure, "SELECT create_hypertable('e2e.metrics', 'time')"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.metrics VALUES ('2026-01-01 00:00:00+00', 'a', 1.5), ('2026-01-02 00:00:00+00', 'b', 2.5)"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "e2e")), "object_name").contains("metrics"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "metrics")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("time", "device", "reading")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT device FROM e2e.metrics WHERE reading > 2"))));
            Assert.assertEquals("2", first(ok("count", service.execute(configure, "SELECT COUNT(*) AS total FROM e2e.metrics"))));

            ok("drop schema", service.execute(configure, "DROP SCHEMA e2e CASCADE"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
