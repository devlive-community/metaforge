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
import static org.devlive.metaforge.test.e2e.PluginE2E.assumeImageSupportsHost;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the DM (Dameng) plugin through the DM JDBC driver against the DM release it documents as
 * verified: a schema, table and columns created with SQL are listed as a database, table and
 * columns, and their rows read back. DM 8 is the only release line with a container image, and it
 * is published for arm64 only, so the test skips on amd64 hosts where DM would run emulated.
 */
@RunWith(Parameterized.class)
public class DmE2ETest
{
    private static final String IMAGE = "hanyf/damengdb:8-kylin10-ubuntu";

    @Parameterized.Parameters(name = "DM {0}")
    public static List<String> versions()
    {
        return List.of("8");
    }

    private final String version;

    public DmE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
        assumeImageSupportsHost(IMAGE);
    }

    @Test
    public void schemasTablesAndColumnsAreListedAndRead()
    {
        try (GenericContainer<?> container = new GenericContainer<>(IMAGE)
                .withExposedPorts(5236)
                .waitingFor(Wait.forLogMessage(".*DM Database is OK.*", 1).withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            PluginService service = new DmService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(5236), "SYSDBA", "SYSDBAPASS", null);
            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.trim().matches("DM Database Server .* V" + version));

            ok("create schema", service.execute(configure, "CREATE SCHEMA E2E AUTHORIZATION SYSDBA"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("E2E"));
            ok("create table", service.execute(configure, "CREATE TABLE E2E.ITEMS (ID INT PRIMARY KEY, NAME VARCHAR(32) NOT NULL)"));
            ok("insert", service.execute(configure, "INSERT INTO E2E.ITEMS VALUES (1, 'a')"));
            ok("insert", service.execute(configure, "INSERT INTO E2E.ITEMS VALUES (2, 'b')"));
            List<String> tables = column(ok("tables", service.getTables(configure, "E2E")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("ITEMS"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "E2E", "ITEMS")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("ID", "NAME")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT NAME FROM E2E.ITEMS WHERE ID = 2"))));

            ok("drop schema", service.execute(configure, "DROP SCHEMA E2E CASCADE"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("E2E"));
        }
    }
}
