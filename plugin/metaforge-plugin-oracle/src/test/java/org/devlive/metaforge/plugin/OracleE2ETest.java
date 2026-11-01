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
 * Runs the Oracle plugin through the Oracle JDBC driver against every Oracle Database release it
 * documents as verified, 23ai and 26ai: a user (schema), table and columns created with SQL are listed as a
 * database, table and columns, and their rows read back.
 */
@RunWith(Parameterized.class)
public class OracleE2ETest
{
    @Parameterized.Parameters(name = "Oracle {0}")
    public static List<String> versions()
    {
        return List.of("23.7", "23.26.0");
    }

    private final String version;

    public OracleE2ETest(String version)
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
        try (GenericContainer<?> container = new GenericContainer<>("gvenzl/oracle-free:" + version + "-slim-faststart")
                .withEnv("ORACLE_PASSWORD", "metaforge")
                .withExposedPorts(1521)
                .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE.*", 1).withStartupTimeout(Duration.ofMinutes(10)))) {
            container.start();
            PluginService service = new OracleService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(1521), "system", "metaforge", "FREEPDB1");
            String reported = first(ok("validator", service.execute(configure, service.validator())));
            // 23ai and 26ai both report the base release 23.0.0.0.0; the full version names the update
            Assert.assertTrue(reported + " is not 23", reported.startsWith("23."));
            String full = first(ok("full version", service.execute(configure, "SELECT version_full FROM product_component_version WHERE product LIKE 'Oracle%Database%'")));
            Assert.assertTrue(full + " is not " + version, full.startsWith(version + "."));

            ok("create user", service.execute(configure, "CREATE USER e2e IDENTIFIED BY metaforge QUOTA UNLIMITED ON USERS"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("E2E"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id NUMBER(10) PRIMARY KEY, name VARCHAR2(32) NOT NULL)"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES (1, 'a')"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES (2, 'b')"));
            List<String> tables = column(ok("tables", service.getTables(configure, "E2E")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("ITEMS"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "E2E", "ITEMS")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("ID", "NAME")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM e2e.items WHERE id = 2"))));

            ok("drop user", service.execute(configure, "DROP USER e2e CASCADE"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("E2E"));
        }
    }
}
