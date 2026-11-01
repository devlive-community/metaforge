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
 * Runs the GreptimeDB plugin through the MySQL JDBC driver against every GreptimeDB release it
 * documents as verified: databases, tables and columns created, read, changed and dropped with
 * SQL.
 */
@RunWith(Parameterized.class)
public class GreptimeDBE2ETest
{
    @Parameterized.Parameters(name = "GreptimeDB {0}")
    public static List<String> versions()
    {
        return List.of("v0.15.5", "v0.17.2");
    }

    private final String version;

    public GreptimeDBE2ETest(String version)
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
        try (GenericContainer<?> container = new GenericContainer<>("greptime/greptimedb:" + version)
                .withCommand("standalone", "start", "--http-addr", "0.0.0.0:4000", "--mysql-addr", "0.0.0.0:4002")
                .withExposedPorts(4000, 4002)
                .waitingFor(Wait.forHttp("/health").forPort(4000).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new GreptimeDBService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(4002), null, null, null);

            ok("validator", service.execute(configure, service.validator()));

            ok("create database", service.execute(configure, "CREATE DATABASE e2e"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (host STRING PRIMARY KEY, ts TIMESTAMP TIME INDEX, cpu DOUBLE)"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "e2e")), "object_name").contains("items"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES ('a', 1700000000000, 0.5), ('b', 1700000000000, 0.7)"));
            Assert.assertEquals("2", first(ok("count", service.execute(configure, "SELECT count(*) AS total FROM e2e.items"))));

            ok("add column", service.execute(configure, "ALTER TABLE e2e.items ADD COLUMN mem DOUBLE"));
            ok("modify column", service.execute(configure, "ALTER TABLE e2e.items MODIFY COLUMN cpu STRING"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("host", "ts", "cpu", "mem")));
            ok("drop column", service.execute(configure, "ALTER TABLE e2e.items DROP COLUMN mem"));
            Assert.assertFalse(column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name").contains("mem"));

            ok("drop table", service.execute(configure, "DROP TABLE e2e.items"));
            Assert.assertFalse(column(ok("tables", service.getTables(configure, "e2e")), "object_name").contains("items"));
            ok("drop database", service.execute(configure, "DROP DATABASE e2e"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
