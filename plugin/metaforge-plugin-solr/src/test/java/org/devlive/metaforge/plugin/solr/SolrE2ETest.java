package org.devlive.metaforge.plugin.solr;

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
 * Runs the Solr plugin through the SolrCloud JDBC driver against every Solr release it documents as
 * verified: documents indexed over HTTP are listed as a collection (table) with its fields, and
 * queried with SQL.
 *
 * <p>The driver reads the nodes' addresses from ZooKeeper and then calls them, so the container
 * registers as localhost on fixed ports 8983 (Solr) and 9983 (its embedded ZooKeeper).</p>
 */
@RunWith(Parameterized.class)
public class SolrE2ETest
{
    @Parameterized.Parameters(name = "Solr {0}")
    public static List<String> versions()
    {
        return List.of("9.4.1", "9.11.0");
    }

    private final String version;

    public SolrE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void collectionsAndFieldsAreListedAndQueried() throws Exception
    {
        try (GenericContainer<?> container = new FixedHostPortGenericContainer<>("solr:" + version)
                .withFixedExposedPort(8983, 8983)
                .withFixedExposedPort(9983, 9983)
                .withEnv("SOLR_HOST", "localhost")
                .withEnv("SOLR_MODULES", "sql")
                .withCommand("solr-foreground", "-c")
                .waitingFor(Wait.forHttp("/solr/admin/info/system").forPort(8983).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            HttpClient http = HttpClient.newHttpClient();
            String endpoint = "http://localhost:8983/solr";
            HttpResponse<String> created = http.send(HttpRequest.newBuilder(URI.create(endpoint
                    + "/admin/collections?action=CREATE&name=items&numShards=1&replicationFactor=1&collection.configName=_default")).build(), HttpResponse.BodyHandlers.ofString());
            Assert.assertEquals(created.body(), 200, created.statusCode());
            HttpResponse<String> indexed = http.send(HttpRequest.newBuilder(URI.create(endpoint + "/items/update?commit=true"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("[{\"id\":\"1\",\"name_s\":\"a\",\"n_i\":1},{\"id\":\"2\",\"name_s\":\"b\",\"n_i\":2}]"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            Assert.assertEquals(indexed.body(), 200, indexed.statusCode());
            PluginService service = new SolrService();
            RuntimeConfigure configure = configure("localhost", 9983, null, null, "items");
            ok("validator", service.execute(configure, service.validator()));
            String system = http.send(HttpRequest.newBuilder(URI.create(endpoint + "/admin/info/system")).build(), HttpResponse.BodyHandlers.ofString()).body();
            Assert.assertTrue(version + " not in " + system, system.contains("\"solr-spec-version\":\"" + version));

            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertEquals(List.of("default"), databases);
            List<String> tables = column(ok("tables", service.getTables(configure, "default")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "default", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name_s", "n_i")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name_s FROM items WHERE n_i = 2"))));
        }
    }
}
