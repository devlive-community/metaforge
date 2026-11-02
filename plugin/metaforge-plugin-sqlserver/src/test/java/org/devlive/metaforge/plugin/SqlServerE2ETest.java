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
import java.util.Map;
import java.util.Optional;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the SQL Server plugin through the Microsoft JDBC driver against every SQL Server release it
 * documents as verified: a schema, table and columns created with SQL are listed as a database,
 * table and columns, and their rows read back.
 */
@RunWith(Parameterized.class)
public class SqlServerE2ETest
{
    @Parameterized.Parameters(name = "SQL Server {0}")
    public static List<String> versions()
    {
        return List.of("2019", "2022");
    }

    private final String version;

    public SqlServerE2ETest(String version)
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
        try (GenericContainer<?> container = new GenericContainer<>("mcr.microsoft.com/mssql/server:" + version + "-latest")
                .withEnv("ACCEPT_EULA", "Y")
                .withEnv("MSSQL_SA_PASSWORD", "MetaForge#2026")
                .withExposedPorts(1433)
                .waitingFor(Wait.forLogMessage(".*Recovery is complete.*", 1).withStartupTimeout(Duration.ofMinutes(10)))) {
            container.start();
            PluginService service = new SqlServerService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(1433), "sa", "MetaForge#2026", "master");
            configure.setEnv(Optional.of(Map.of("trustServerCertificate", "true")));
            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith("Microsoft SQL Server " + version + " "));

            ok("create schema", service.execute(configure, "CREATE SCHEMA e2e"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("e2e"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id INT PRIMARY KEY, name NVARCHAR(32) NOT NULL)"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES (1, 'a'), (2, 'b')"));
            List<String> tables = column(ok("tables", service.getTables(configure, "e2e")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM e2e.items WHERE id = 2"))));

            ok("drop table", service.execute(configure, "DROP TABLE e2e.items"));
            ok("drop schema", service.execute(configure, "DROP SCHEMA e2e"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
