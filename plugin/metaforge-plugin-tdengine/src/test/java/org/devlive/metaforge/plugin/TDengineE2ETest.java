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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the TDengine plugin through the TDengine JDBC driver's REST connection against every
 * TDengine release it documents as verified: a database, super table and sub table created with SQL
 * are listed as a database, tables and columns, and their rows read back.
 */
@RunWith(Parameterized.class)
public class TDengineE2ETest
{
    @Parameterized.Parameters(name = "TDengine {0}")
    public static List<String> versions()
    {
        return List.of("3.1.1.12", "3.3.6.13");
    }

    private final String version;

    public TDengineE2ETest(String version)
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
        try (GenericContainer<?> container = new GenericContainer<>("tdengine/tdengine:" + version)
                .withExposedPorts(6041)
                .waitingFor(Wait.forHttp("/-/ping").forPort(6041).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            // taosAdapter answers before the server has registered its dnode; wait until the dnode is ready
            long deadline = System.currentTimeMillis() + Duration.ofMinutes(2).toMillis();
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest dnodes = HttpRequest.newBuilder(URI.create("http://" + container.getHost() + ":" + container.getMappedPort(6041) + "/rest/sql"))
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("root:taosdata".getBytes(StandardCharsets.UTF_8)))
                    .POST(HttpRequest.BodyPublishers.ofString("show dnodes"))
                    .build();
            while (!http.send(dnodes, HttpResponse.BodyHandlers.ofString()).body().contains("\"ready\"")) {
                Assert.assertTrue("dnode not ready", System.currentTimeMillis() < deadline);
                Thread.sleep(1000);
            }
            PluginService service = new TDengineService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(6041), "root", "taosdata", null);
            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version));

            ok("create database", service.execute(configure, "CREATE DATABASE e2e"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("e2e"));
            ok("create stable", service.execute(configure, "CREATE STABLE e2e.meters (ts TIMESTAMP, reading DOUBLE) TAGS (device NCHAR(16))"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.d1 USING e2e.meters TAGS ('a') VALUES ('2026-01-01 00:00:00', 1.5)"));
            ok("insert", service.execute(configure, "INSERT INTO e2e.d2 USING e2e.meters TAGS ('b') VALUES ('2026-01-01 00:00:00', 2.5)"));
            List<String> tables = column(ok("tables", service.getTables(configure, "e2e")), "object_name");
            Assert.assertTrue(tables.toString(), tables.containsAll(List.of("meters", "d1", "d2")));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "meters")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("ts", "reading", "device")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT device FROM e2e.meters WHERE reading > 2"))));

            ok("drop database", service.execute(configure, "DROP DATABASE e2e"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("e2e"));
        }
    }
}
