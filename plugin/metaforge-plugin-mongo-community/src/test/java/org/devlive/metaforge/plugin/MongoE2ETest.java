package org.devlive.metaforge.plugin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
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
 * Runs the MongoDB plugin through MetaForge's MongoDB JDBC driver against every MongoDB release
 * line it documents as verified: documents written by another client are listed as databases,
 * collections and fields, and read back with SQL.
 */
@RunWith(Parameterized.class)
public class MongoE2ETest
{
    @Parameterized.Parameters(name = "MongoDB {0}")
    public static List<String> versions()
    {
        return List.of("6.0", "7.0");
    }

    private final String version;

    public MongoE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void collectionsAreListedAndRead()
    {
        try (GenericContainer<?> container = new GenericContainer<>("mongo:" + version)
                .withExposedPorts(27017)
                .waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*", 1).withStartupTimeout(Duration.ofMinutes(3)))) {
            container.start();
            try (MongoClient client = MongoClients.create("mongodb://" + container.getHost() + ":" + container.getMappedPort(27017))) {
                client.getDatabase("shop").getCollection("orders").insertMany(List.of(
                        new Document("item", "pen").append("qty", 3),
                        new Document("item", "ink").append("qty", 5).append("note", "blue")));
            }
            PluginService service = new MongoCommunityService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(27017), null, null, "shop");
            configure.setType(service.connectType());

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version + "."));

            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("shop"));
            Assert.assertEquals(List.of("orders"), column(ok("tables", service.getTables(configure, "shop")), "object_name"));
            List<String> fields = column(ok("columns", service.getColumns(configure, "shop", "orders")), "object_name");
            Assert.assertTrue(fields.toString(), fields.containsAll(List.of("_id", "item", "qty", "note")));

            List<String> items = column(ok("select", service.execute(configure, "SELECT item FROM orders WHERE qty > 4")), "item");
            Assert.assertEquals(List.of("ink"), items);
        }
    }
}
