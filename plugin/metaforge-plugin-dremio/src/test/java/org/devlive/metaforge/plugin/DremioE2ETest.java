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
 * Runs the Dremio plugin through the Arrow Flight SQL JDBC driver against every Dremio release it documents as
 * verified: a view created with SQL in the user's home space is listed as a table of a database with
 * its columns, and its rows read back.
 */
@RunWith(Parameterized.class)
public class DremioE2ETest
{
    @Parameterized.Parameters(name = "Dremio {0}")
    public static List<String> versions()
    {
        return List.of("25.2", "26.0");
    }

    private final String version;

    public DremioE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void tablesAndColumnsAreListedAndRead() throws Exception
    {
        try (GenericContainer<?> container = new GenericContainer<>("dremio/dremio-oss:" + version)
                .withExposedPorts(9047, 32010)
                .waitingFor(Wait.forHttp("/apiv2/server_status").forPort(9047).withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            HttpResponse<String> admin = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://" + container.getHost() + ":" + container.getMappedPort(9047) + "/apiv2/bootstrap/firstuser"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "_dremionull")
                    .PUT(HttpRequest.BodyPublishers.ofString("{\"userName\":\"metaforge\",\"firstName\":\"Meta\",\"lastName\":\"Forge\",\"email\":\"e2e@metaforge.dev\",\"createdBy\":\"metaforge\",\"password\":\"metaforge123\"}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            Assert.assertEquals(admin.body(), 200, admin.statusCode());
            PluginService service = new DremioService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(32010), "metaforge", "metaforge123", null);
            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version + "."));

            ok("create table", service.execute(configure, "CREATE TABLE \"$scratch\".items AS SELECT * FROM (VALUES (1, 'a'), (2, 'b')) AS t(id, name)"));
            // The user's home space lists a view as soon as it is created; scratch tables show up later
            ok("create view", service.execute(configure, "CREATE VIEW \"@metaforge\".items AS SELECT * FROM \"$scratch\".items"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("@metaforge"));
            List<String> tables = column(ok("tables", service.getTables(configure, "@metaforge")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("items"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "@metaforge", "items")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM \"@metaforge\".items WHERE id = 2"))));

            ok("drop view", service.execute(configure, "DROP VIEW \"@metaforge\".items"));
            ok("drop table", service.execute(configure, "DROP TABLE \"$scratch\".items"));
        }
    }
}
