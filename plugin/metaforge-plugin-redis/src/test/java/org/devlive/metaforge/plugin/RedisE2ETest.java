package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementEntry;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
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
import java.util.stream.Collectors;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the Redis plugin through MetaForge's Redis JDBC driver against every Redis release line it
 * documents as verified. Data is written with plain Redis commands, as other clients write it, then
 * read back with SQL and through the key browser that draws the source's tree.
 */
@RunWith(Parameterized.class)
public class RedisE2ETest
{
    @Parameterized.Parameters(name = "Redis {0}")
    public static List<String> versions()
    {
        return List.of("6.2", "7.4");
    }

    private final String version;

    public RedisE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void keysWrittenByOtherClientsAreReadWithSqlAndBrowsed()
    {
        try (GenericContainer<?> container = new GenericContainer<>("redis:" + version)
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))) {
            container.start();
            PluginService service = new RedisService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(6379), null, null, "0");

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version + "."));

            ok("rpush", service.execute(configure, "RPUSH test:list a b c"));
            ok("set", service.execute(configure, "SET test:string \"hello world\""));
            ok("hset", service.execute(configure, "HSET test:hash name metaforge"));
            Assert.assertEquals("hello world", first(ok("get", service.execute(configure, "GET test:string"))));

            Response list = ok("select list", service.execute(configure, "SELECT * FROM `test:list`"));
            Assert.assertEquals(List.of("test:list"), column(list, "key"));
            Assert.assertEquals(List.of("LIST"), column(list, "type"));
            Assert.assertTrue(column(list, "value").get(0), column(list, "value").get(0).contains("b"));
            Response value = ok("select string", service.execute(configure, "SELECT value FROM test:string"));
            Assert.assertEquals(List.of("hello world"), column(value, "value"));
            Assert.assertNull("only the selected column", ((com.fasterxml.jackson.databind.JsonNode) value.getColumns().get(0)).get("type"));
            Assert.assertEquals(List.of("test:hash", "test:list", "test:string"),
                    column(ok("select all", service.execute(configure, "SELECT `key` FROM `test:*`")), "key").stream().sorted().collect(Collectors.toList()));
            // A pattern that matches nothing is an empty result with its columns, not an error.
            Assert.assertTrue(ok("select none", service.execute(configure, "SELECT * FROM `missing:*`")).getColumns().isEmpty());

            ManagementPage namespaces = service.managementNamespaces(configure, new ManagementRequest());
            Assert.assertEquals("0", namespaces.getNamespace());
            ManagementRequest browse = new ManagementRequest();
            browse.setNamespace("0");
            Map<String, ManagementEntry> keys = service.managementList(configure, browse).getEntries().stream()
                    .collect(Collectors.toMap(ManagementEntry::getName, entry -> entry));
            Assert.assertEquals(keys.keySet().toString(), "list", keys.get("test:list").getType());
            Assert.assertEquals("hash", keys.get("test:hash").getType());

            ManagementRequest open = new ManagementRequest();
            open.setNamespace("0");
            open.setKey(keys.get("test:list").getId());
            ManagementDetail detail = service.managementDetail(configure, open);
            Assert.assertEquals(List.of("a", "b", "c"), detail.getRows().stream().map(row -> row.get("value")).collect(Collectors.toList()));
        }
    }
}
