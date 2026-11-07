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
 * Runs the Kyuubi plugin through the Kyuubi Hive JDBC driver against every Kyuubi release it
 * documents as verified, with its Spark SQL engine on a local Spark: a database and table created
 * with SQL are listed as a database, table and columns, and their rows read back.
 *
 * <p>Kyuubi publishes amd64 images only.</p>
 */
@RunWith(Parameterized.class)
public class KyuubiE2ETest
{
    @Parameterized.Parameters(name = "Kyuubi {0}")
    public static List<String> versions()
    {
        return List.of("1.10.3", "1.11.1");
    }

    private final String version;

    public KyuubiE2ETest(String version)
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
        try (GenericContainer<?> container = new GenericContainer<>("apache/kyuubi:" + version + "-spark")
                .withCreateContainerCmdModifier(command -> command.withPlatform("linux/amd64"))
                .withExposedPorts(10009)
                .waitingFor(Wait.forListeningPorts(10009).withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            PluginService service = new KyuubiService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(10009), "metaforge", null, null);

            // The first statement starts a Spark engine, which takes a while on an emulated amd64 image
            ok("validator", service.execute(configure, service.validator()));
            Assert.assertEquals(version, first(ok("kyuubi version", service.execute(configure, "SELECT kyuubi_version()"))));

            ok("create database", service.execute(configure, "CREATE DATABASE e2e"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("e2e"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id INT, name STRING) USING parquet"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.items VALUES (1, 'a'), (2, 'b')"));
            List<String> tables = column(ok("tables", service.getTables(configure, "e2e")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM e2e.items WHERE id = 2"))));

            ok("drop database", service.execute(configure, "DROP DATABASE e2e CASCADE"));
        }
    }
}
