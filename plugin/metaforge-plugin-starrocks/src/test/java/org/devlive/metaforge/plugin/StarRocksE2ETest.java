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
import java.util.Map;
import java.util.Optional;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the StarRocks plugin through the MySQL JDBC driver against every StarRocks release it
 * documents as verified: a database, table and columns created with SQL are listed as a database,
 * table and columns, and their rows read back.
 */
@RunWith(Parameterized.class)
public class StarRocksE2ETest
{
    @Parameterized.Parameters(name = "StarRocks {0}")
    public static List<String> versions()
    {
        return List.of("3.5", "4.0");
    }

    private final String version;

    public StarRocksE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void databasesTablesAndColumnsAreListedAndRead() throws Exception
    {
        try (GenericContainer<?> container = new GenericContainer<>("starrocks/allin1-ubuntu:" + version + "-latest")
                .withExposedPorts(9030)
                .waitingFor(Wait.forListeningPorts(9030).withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            PluginService service = new StarRocksService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(9030), "root", "", null);
            configure.setEnv(Optional.of(Map.of("useSSL", "false")));
            // The frontend accepts connections before its backend has registered; retry the check until it runs
            long deadline = System.currentTimeMillis() + Duration.ofMinutes(3).toMillis();
            Response checked = service.execute(configure, service.validator());
            while (!Boolean.TRUE.equals(checked.getIsSuccessful()) && System.currentTimeMillis() < deadline) {
                Thread.sleep(2000);
                checked = service.execute(configure, service.validator());
            }
            String reported = first(ok("validator", checked));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version + "."));

            ok("create database", service.execute(configure, "CREATE DATABASE e2e"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("e2e"));
            String table = "CREATE TABLE e2e.items (id INT, name VARCHAR(32)) DUPLICATE KEY(id) DISTRIBUTED BY HASH(id) BUCKETS 1 PROPERTIES (\"replication_num\" = \"1\")";
            // Tables need a live backend, which can register after the frontend answers queries
            Response created = service.execute(configure, table);
            while (!Boolean.TRUE.equals(created.getIsSuccessful()) && System.currentTimeMillis() < deadline) {
                Thread.sleep(2000);
                created = service.execute(configure, table);
            }
            ok("create table", created);
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
