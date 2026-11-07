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
 * Runs the OceanBase plugin through the OceanBase JDBC driver against every OceanBase CE release
 * line it documents as verified, in its MySQL mode: a database, table and columns created with
 * SQL are listed as a database, table and columns, and their rows read back.
 */
@RunWith(Parameterized.class)
public class OceanBaseE2ETest
{
    @Parameterized.Parameters(name = "OceanBase {0}")
    public static List<String> versions()
    {
        return List.of("4.2.5", "4.3.5", "4.4.2");
    }

    private final String version;

    public OceanBaseE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void databasesTablesAndColumnsAreListedAndRead()
    {
        try (GenericContainer<?> container = new GenericContainer<>("oceanbase/oceanbase-ce:" + version + "-lts")
                // The slim mode boots one observer with little memory
                .withEnv("MODE", "slim")
                .withExposedPorts(2881)
                .waitingFor(Wait.forLogMessage(".*boot success.*", 1).withStartupTimeout(Duration.ofMinutes(10)))) {
            container.start();
            PluginService service = new OceanBaseService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(2881), "root@test", "", "test");

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.contains("-v" + version + "."));

            ok("create database", service.execute(configure, "CREATE DATABASE e2e"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("e2e"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id INT PRIMARY KEY, name VARCHAR(32) NOT NULL)"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES (1, 'a'), (2, 'b')"));
            List<String> tables = column(ok("tables", service.getTables(configure, "e2e")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM e2e.items WHERE id = 2"))));

            ok("drop database", service.execute(configure, "DROP DATABASE e2e"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
