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
import java.time.Duration;
import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the Elasticsearch 8.x plugin through the Elasticsearch SQL JDBC driver against every
 * Elasticsearch release it documents as verified: an index written over HTTP is listed as a table
 * of the cluster with its fields as columns, and its documents queried with SQL.
 */
@RunWith(Parameterized.class)
public class ElasticSearch8xE2ETest
{
    @Parameterized.Parameters(name = "Elasticsearch {0}")
    public static List<String> versions()
    {
        return List.of("8.15.5", "8.19.5");
    }

    private final String version;

    public ElasticSearch8xE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void indicesAreListedAndQueried() throws Exception
    {
        try (GenericContainer<?> container = new GenericContainer<>("docker.elastic.co/elasticsearch/elasticsearch:" + version)
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
                // The SQL JDBC driver needs a Platinum license; a trial license grants it
                .withEnv("xpack.license.self_generated.type", "trial")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withExposedPorts(9200)
                .waitingFor(Wait.forHttp("/_cluster/health?wait_for_status=yellow").forPort(9200).withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            String endpoint = "http://" + container.getHost() + ":" + container.getMappedPort(9200);
            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> written = http.send(HttpRequest.newBuilder(URI.create(endpoint + "/items/_bulk?refresh=true"))
                    .header("Content-Type", "application/x-ndjson")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"index\":{\"_id\":\"1\"}}\n{\"id\":1,\"name\":\"a\"}\n{\"index\":{\"_id\":\"2\"}}\n{\"id\":2,\"name\":\"b\"}\n"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            Assert.assertEquals(written.body(), 200, written.statusCode());
            PluginService service = new ElasticSearch8xService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(9200), null, null, null);
            ok("validator", service.execute(configure, service.validator()));
            String root = http.send(HttpRequest.newBuilder(URI.create(endpoint)).build(), HttpResponse.BodyHandlers.ofString()).body();
            Assert.assertTrue(root + " is not " + version, root.contains("\"number\" : \"" + version + "\""));

            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("docker-cluster"));
            List<String> tables = column(ok("tables", service.getTables(configure, "docker-cluster")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "docker-cluster", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM items WHERE id = 2"))));
            Assert.assertEquals("2", first(ok("count", service.execute(configure, "SELECT COUNT(*) AS total FROM items"))));
        }
    }
}
