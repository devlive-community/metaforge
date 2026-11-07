package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * Runs the Pinot plugin through the Pinot JDBC driver against every Pinot release it documents as
 * verified: the batch quick start's tables are listed with their columns and queried with SQL.
 *
 * <p>Brokers register with their container address, so the connection names the broker itself
 * ({@code brokers=localhost:8000}) on a fixed port.</p>
 */
@RunWith(Parameterized.class)
public class PinotE2ETest
{
    @Parameterized.Parameters(name = "Pinot {0}")
    public static List<String> versions()
    {
        return List.of("1.4.0", "1.5.0");
    }

    private final String version;

    public PinotE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void tablesAndColumnsAreListedAndQueried()
            throws Exception
    {
        try (GenericContainer<?> container = new FixedHostPortGenericContainer<>("apachepinot/pinot:" + version)
                .withFixedExposedPort(8000, 8000)
                .withExposedPorts(9000)
                .withCommand("QuickStart", "-type", "batch")
                .waitingFor(Wait.forHttp("/tables").forPort(9000).withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            String controller = "http://" + container.getHost() + ":" + container.getMappedPort(9000);
            HttpRequest count = HttpRequest.newBuilder(URI.create("http://localhost:8000/query/sql"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"sql\":\"SELECT COUNT(*) FROM baseballStats\"}"))
                    .build();
            // The broker starts after the controller, and the quick start loads its tables' segments last
            long deadline = System.currentTimeMillis() + Duration.ofMinutes(3).toMillis();
            while (!loaded(http, count)) {
                Assert.assertTrue("baseballStats not loaded", System.currentTimeMillis() < deadline);
                Thread.sleep(2000);
            }
            PluginService service = new PinotService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(9000), null, null, null);
            configure.setEnv(Optional.of(Map.of("brokers", "localhost:8000")));

            ok("validator", service.execute(configure, service.validator()));
            String reported = http.send(HttpRequest.newBuilder(URI.create(controller + "/version")).build(), HttpResponse.BodyHandlers.ofString()).body();
            Assert.assertTrue(reported + " is not " + version, reported.contains("\"" + version + "-"));

            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertFalse(databases.toString(), databases.isEmpty());
            List<String> tables = column(ok("tables", service.getTables(configure, databases.get(0))), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("baseballStats"));
            List<String> columns = column(ok("columns", service.getColumns(configure, databases.get(0), "baseballStats")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("playerName", "homeRuns")));
            Assert.assertEquals("97889", first(ok("count", service.execute(configure, "SELECT COUNT(*) FROM baseballStats"))));
        }
    }

    private static boolean loaded(HttpClient http, HttpRequest count)
            throws InterruptedException
    {
        try {
            String body = http.send(count, HttpResponse.BodyHandlers.ofString()).body();
            return body.contains("\"rows\":[[97889]]");
        }
        catch (IOException error) {
            return false;
        }
    }
}
