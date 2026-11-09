package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.sql.SqlLimit;
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
 * Runs the ClickHouse plugin through its JDBC driver against every ClickHouse release line it
 * documents as verified: version check, then databases, tables and columns created, read,
 * changed and dropped with SQL.
 */
@RunWith(Parameterized.class)
public class ClickHouseE2ETest
{
    @Parameterized.Parameters(name = "ClickHouse {0}")
    public static List<String> versions()
    {
        return List.of("24.8", "25.8", "26.3.60.2", "26.8.34.13");
    }

    private final String version;

    public ClickHouseE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void databasesTablesAndColumnsAreManagedWithSql()
    {
        try (GenericContainer<?> container = new GenericContainer<>("clickhouse/clickhouse-server:" + version)
                .withEnv("CLICKHOUSE_USER", "metaforge")
                .withEnv("CLICKHOUSE_PASSWORD", "metaforge")
                .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1")
                .withExposedPorts(8123)
                .waitingFor(Wait.forHttp("/ping").forPort(8123).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new ClickHouseService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(8123), "metaforge", "metaforge", null);

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version.split("\\.")[0] + "." + version.split("\\.")[1]));

            ok("create database", service.execute(configure, "CREATE DATABASE e2e"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));

            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id UInt32, name String) ENGINE = MergeTree ORDER BY id"));
            // The query page's row cap: LIMIT in the SQL plus the driver's row limit, cut and marked as truncated
            configure.setMaxRows(10);
            String capped = SqlLimit.append("SELECT number FROM numbers(50)", 11).orElseThrow();
            Response cut = ok("capped select", service.execute(configure, capped));
            Assert.assertEquals(10, cut.getColumns().size());
            Assert.assertEquals(Boolean.TRUE, cut.getTruncated());
            Response driverCut = ok("driver capped select", service.execute(configure, "SELECT number FROM numbers(50)"));
            Assert.assertEquals(10, driverCut.getColumns().size());
            Assert.assertEquals(Boolean.TRUE, driverCut.getTruncated());
            configure.setMaxRows(0);

            List<String> objects = column(ok("tables", service.getTables(configure, "e2e")), "object_name");
            Assert.assertTrue(objects.contains("items"));
            // Built-in functions are not objects of the database
            Assert.assertFalse(objects.toString(), objects.contains("BIT_AND"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES (1, 'a'), (2, 'b')"));
            Assert.assertEquals("2", first(ok("count", service.execute(configure, "SELECT count() AS total FROM e2e.items"))));

            ok("add column", service.execute(configure, "ALTER TABLE e2e.items ADD COLUMN created DateTime DEFAULT now()"));
            ok("modify column", service.execute(configure, "ALTER TABLE e2e.items MODIFY COLUMN name Nullable(String)"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name", "created")));
            ok("drop column", service.execute(configure, "ALTER TABLE e2e.items DROP COLUMN created"));
            Assert.assertFalse(column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name").contains("created"));

            // The index page reads ClickHouse system tables, never MySQL's information_schema.STATISTICS.
            ok("add skipping index", service.execute(configure, "ALTER TABLE e2e.items ADD INDEX idx_name name TYPE bloom_filter GRANULARITY 4"));
            List<String> indexes = column(ok("index details", service.getIndexDetails(configure, "e2e", "items")), "index_name");
            Assert.assertTrue(indexes.toString(), indexes.containsAll(List.of("PRIMARY", "idx_name")));

            ok("drop table", service.execute(configure, "DROP TABLE e2e.items"));
            Assert.assertFalse(column(ok("tables", service.getTables(configure, "e2e")), "object_name").contains("items"));
            ok("drop database", service.execute(configure, "DROP DATABASE e2e"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
