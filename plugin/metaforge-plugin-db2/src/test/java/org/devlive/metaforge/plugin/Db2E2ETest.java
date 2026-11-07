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
 * Runs the Db2 plugin through IBM's JDBC driver against every Db2 release line it documents as
 * verified: a schema, table and columns created with SQL are listed as a database, table and
 * columns, and their rows read back.
 *
 * <p>Db2 Community Edition is published for amd64 only and needs a privileged container.</p>
 */
@RunWith(Parameterized.class)
public class Db2E2ETest
{
    @Parameterized.Parameters(name = "Db2 {0}")
    public static List<String> versions()
    {
        return List.of("11.5.9.0", "12.1.5.0");
    }

    private final String version;

    public Db2E2ETest(String version)
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
        try (GenericContainer<?> container = new GenericContainer<>("icr.io/db2_community/db2:" + version)
                .withCreateContainerCmdModifier(command -> command.withPlatform("linux/amd64"))
                .withPrivilegedMode(true)
                .withEnv("LICENSE", "accept")
                .withEnv("DB2INST1_PASSWORD", "metaforge")
                .withEnv("DBNAME", "testdb")
                .withEnv("ARCHIVE_LOGS", "false")
                .withEnv("AUTOCONFIG", "false")
                .withExposedPorts(50000)
                .waitingFor(Wait.forLogMessage(".*Setup has completed.*", 1).withStartupTimeout(Duration.ofMinutes(15)))) {
            container.start();
            PluginService service = new Db2Service();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(50000), "db2inst1", "metaforge", "testdb");

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version));

            ok("create schema", service.execute(configure, "CREATE SCHEMA e2e"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("E2E"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id INT NOT NULL PRIMARY KEY, name VARCHAR(32) NOT NULL)"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES (1, 'a'), (2, 'b')"));
            List<String> tables = column(ok("tables", service.getTables(configure, "E2E")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("ITEMS"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "E2E", "ITEMS")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("ID", "NAME")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM e2e.items WHERE id = 2"))));

            ok("drop table", service.execute(configure, "DROP TABLE e2e.items"));
            ok("drop schema", service.execute(configure, "DROP SCHEMA e2e RESTRICT"));
        }
    }
}
