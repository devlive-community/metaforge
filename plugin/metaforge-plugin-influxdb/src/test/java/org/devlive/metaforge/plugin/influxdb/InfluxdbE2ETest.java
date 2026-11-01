package org.devlive.metaforge.plugin.influxdb;

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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the InfluxDB plugin through the InfluxDB JDBC driver against every InfluxDB release it
 * documents as verified: points written over HTTP are listed as a database, measurement and fields,
 * and read back with InfluxQL.
 */
@RunWith(Parameterized.class)
public class InfluxdbE2ETest
{
    @Parameterized.Parameters(name = "InfluxDB {0}")
    public static List<String> versions()
    {
        return List.of("1.8", "1.11");
    }

    private final String version;

    public InfluxdbE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void measurementsAreListedAndRead() throws Exception
    {
        try (GenericContainer<?> container = new GenericContainer<>("influxdb:" + version)
                .withExposedPorts(8086)
                .waitingFor(Wait.forLogMessage(".*Listening on HTTP.*", 1).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            String endpoint = "http://" + container.getHost() + ":" + container.getMappedPort(8086);
            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> created = http.send(HttpRequest.newBuilder(URI.create(endpoint + "/query?q=" + URLEncoder.encode("CREATE DATABASE e2e", StandardCharsets.UTF_8)))
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            Assert.assertEquals(created.body(), 200, created.statusCode());
            HttpResponse<String> written = http.send(HttpRequest.newBuilder(URI.create(endpoint + "/write?db=e2e"))
                    .POST(HttpRequest.BodyPublishers.ofString("cpu,host=a usage=1.5 1700000000000000000\ncpu,host=b usage=2.5 1700000001000000000\n")).build(),
                    HttpResponse.BodyHandlers.ofString());
            Assert.assertEquals(written.body(), 204, written.statusCode());
            PluginService service = new InfluxdbService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(8086), null, null, "e2e");
            String reported = column(ok("validator", service.execute(configure, service.validator())), "Version").get(0);
            // 1.11 builds prefix their version with v
            Assert.assertTrue(reported + " is not " + version, reported.replaceFirst("^v", "").startsWith(version + "."));

            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("e2e"));
            List<String> tables = column(ok("tables", service.getTables(configure, "e2e")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("cpu"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "e2e", "cpu")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("host", "usage")));
            Assert.assertEquals(List.of("2.5"), column(ok("select", service.execute(configure, "SELECT usage FROM cpu WHERE host = 'b'")), "usage"));
        }
    }
}
