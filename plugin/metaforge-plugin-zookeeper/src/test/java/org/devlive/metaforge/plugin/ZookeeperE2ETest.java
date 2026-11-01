package org.devlive.metaforge.plugin;

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
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the ZooKeeper plugin through MetaForge's ZooKeeper JDBC driver against every ZooKeeper
 * release line it documents as verified: znodes created, read, changed and dropped as databases,
 * tables, columns and values with SQL.
 */
@RunWith(Parameterized.class)
public class ZookeeperE2ETest
{
    @Parameterized.Parameters(name = "ZooKeeper {0}")
    public static List<String> versions()
    {
        return List.of("3.8", "3.9");
    }

    private final String version;

    public ZookeeperE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void znodesAreManagedAsDatabasesTablesAndColumns()
    {
        try (GenericContainer<?> container = new GenericContainer<>("zookeeper:" + version)
                .withExposedPorts(2181)
                .waitingFor(Wait.forLogMessage(".*(binding to port|Started AdminServer|PrepRequestProcessor).*", 1).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new ZookeeperService();
            RuntimeConfigure configure = configure(container.getHost() + ":" + container.getMappedPort(2181), 0, null, null, null);
            configure.setPort(null);

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version + "."));

            ok("create database", service.execute(configure, "CREATE DATABASE app"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("app"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("zookeeper"));
            ok("create table", service.execute(configure, "CREATE TABLE app.config (host VARCHAR(64), port VARCHAR(8))"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "app")), "object_name").contains("config"));

            ok("insert", service.execute(configure, "INSERT INTO app.config (host, port) VALUES ('db.local', '3306')"));
            Response row = ok("select", service.execute(configure, "SELECT host, port FROM app.config WHERE port = '3306'"));
            Assert.assertEquals("db.local", first(row));
            ok("update", service.execute(configure, "UPDATE app.config SET port = '3307'"));
            Assert.assertEquals(List.of("3307"), column(ok("select", service.execute(configure, "SELECT port FROM app.config")), "port"));

            ok("add column", service.execute(configure, "ALTER TABLE app.config ADD COLUMN timeout VARCHAR(8)"));
            ok("rename column", service.execute(configure, "ALTER TABLE app.config RENAME COLUMN host TO address"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "app", "config")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("address", "port", "timeout")));
            Assert.assertEquals("db.local", first(ok("renamed value", service.execute(configure, "SELECT address FROM app.config"))));
            ok("drop column", service.execute(configure, "ALTER TABLE app.config DROP COLUMN timeout"));
            Assert.assertFalse(column(ok("columns", service.getColumns(configure, "app", "config")), "object_name").contains("timeout"));

            ok("drop table", service.execute(configure, "DROP TABLE app.config"));
            Assert.assertFalse(column(ok("tables", service.getTables(configure, "app")), "object_name").contains("config"));
            ok("drop database", service.execute(configure, "DROP DATABASE app"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("app"));
        }
    }
}
