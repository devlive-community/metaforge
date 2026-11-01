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
 * Runs the Cassandra plugin through the cassandra-jdbc-wrapper driver against every Cassandra release it
 * documents as verified: keyspaces, tables and columns created, read, changed and dropped with CQL.
 */
@RunWith(Parameterized.class)
public class CassandraE2ETest
{
    @Parameterized.Parameters(name = "Cassandra {0}")
    public static List<String> versions()
    {
        return List.of("4.1", "5.0");
    }

    private final String version;

    public CassandraE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void keyspacesTablesAndColumnsAreManagedWithCql()
    {
        try (GenericContainer<?> container = new GenericContainer<>("cassandra:" + version)
                .withExposedPorts(9042)
                .waitingFor(Wait.forLogMessage(".*Starting listening for CQL clients.*", 1).withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            PluginService service = new CassandraService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(9042), null, null, null);

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version + "."));

            ok("create keyspace", service.execute(configure, "CREATE KEYSPACE app WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("app"));
            ok("alter keyspace", service.execute(configure, "ALTER KEYSPACE app WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1} AND durable_writes = true"));
            ok("create table", service.execute(configure, "CREATE TABLE app.users (id int PRIMARY KEY, name text)"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "app")), "object_name").contains("users"));

            ok("insert", service.execute(configure, "INSERT INTO app.users (id, name) VALUES (1, 'ann')"));
            Assert.assertEquals("ann", first(ok("select", service.execute(configure, "SELECT name FROM app.users WHERE id = 1"))));

            ok("add column", service.execute(configure, "ALTER TABLE app.users ADD age int"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "app", "users")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name", "age")));
            ok("drop column", service.execute(configure, "ALTER TABLE app.users DROP age"));
            Assert.assertFalse(column(ok("columns", service.getColumns(configure, "app", "users")), "object_name").contains("age"));

            ok("drop table", service.execute(configure, "DROP TABLE app.users"));
            Assert.assertFalse(column(ok("tables", service.getTables(configure, "app")), "object_name").contains("users"));
            ok("drop keyspace", service.execute(configure, "DROP KEYSPACE app"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("app"));
        }
    }
}
