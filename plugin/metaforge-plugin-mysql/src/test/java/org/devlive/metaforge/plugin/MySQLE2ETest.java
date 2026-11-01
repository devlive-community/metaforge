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
 * Runs the MySQL plugin through its JDBC driver against every MySQL release line it documents as
 * verified: databases, tables and columns managed with SQL, and multi-statement scripts applied
 * as one transaction.
 */
@RunWith(Parameterized.class)
public class MySQLE2ETest
{
    @Parameterized.Parameters(name = "MySQL {0}")
    public static List<String> versions()
    {
        return List.of("8.0", "8.4", "9.4");
    }

    private final String version;

    public MySQLE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void databasesTablesColumnsAndScriptsAreManagedWithSql()
    {
        try (GenericContainer<?> container = new GenericContainer<>("mysql:" + version)
                .withEnv("MYSQL_ROOT_PASSWORD", "metaforge")
                .withExposedPorts(3306)
                .waitingFor(Wait.forLogMessage(".*ready for connections.*port: 3306.*", 1).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            PluginService service = new MySQLService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(3306), "root", "metaforge", null);
            configure.setEnv(Optional.of(Map.of("useSSL", "false", "allowPublicKeyRetrieval", "true")));

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version + "."));

            ok("create database", service.execute(configure, "CREATE DATABASE e2e"));
            Response databases = ok("databases", service.getDatabases(configure));
            Assert.assertTrue(databases.getHeaders() + " " + databases.getColumns(), column(databases, "object_name").contains("e2e"));
            ok("create table", service.execute(configure, "CREATE TABLE e2e.items (id INT PRIMARY KEY, name VARCHAR(32) NOT NULL);"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "e2e")), "object_name").contains("items"));

            Response script = ok("script", service.execute(configure, "INSERT INTO e2e.items VALUES (1, 'a');\nINSERT INTO e2e.items VALUES (2, 'b');"));
            Assert.assertEquals("2", first(script));
            Response failed = service.execute(configure, "INSERT INTO e2e.items VALUES (3, 'c');\nINSERT INTO e2e.items VALUES (1, 'duplicate');");
            Assert.assertFalse("a failing script reports failure", Boolean.TRUE.equals(failed.getIsSuccessful()));
            Assert.assertEquals("a failing script changes nothing", "2", first(ok("count", service.execute(configure, "SELECT COUNT(*) AS total FROM e2e.items"))));

            ok("add column", service.execute(configure, "ALTER TABLE e2e.items ADD COLUMN created DATETIME NULL"));
            ok("modify column", service.execute(configure, "ALTER TABLE e2e.items MODIFY COLUMN name VARCHAR(64) NULL"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name", "created")));
            ok("drop column", service.execute(configure, "ALTER TABLE e2e.items DROP COLUMN created"));
            Assert.assertFalse(column(ok("columns", service.getColumns(configure, "e2e", "items")), "object_name").contains("created"));

            ok("drop table", service.execute(configure, "DROP TABLE e2e.items"));
            ok("drop database", service.execute(configure, "DROP DATABASE e2e"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
