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
import java.util.stream.Collectors;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the Redis plugin through MetaForge's Redis JDBC driver against every Redis release line it
 * documents as verified. Data is written with plain Redis commands, as other clients write it, then
 * read back with SQL and listed as databases, tables and columns like any other source.
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

            // Browsed like MySQL: databases, keys as tables, key/type/value as columns, and the workbench's qualified SELECT.
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").containsAll(List.of("0", "1", "15")));
            Assert.assertEquals(List.of("test:hash", "test:list", "test:string"), column(ok("tables", service.getTables(configure, "0")), "object_name"));
            Assert.assertTrue(column(ok("tables of db 1", service.getTables(configure, "1")), "object_name").isEmpty());
            Assert.assertEquals(List.of("key", "type", "value"), column(ok("columns", service.getColumns(configure, "0", "test:list")), "object_name"));
            Response scoped = ok("workbench select", service.execute(configure, "SELECT * FROM `0`.`test:list` LIMIT 100"));
            Assert.assertEquals(List.of("test:list"), column(scoped, "key"));
            Assert.assertEquals(1, ok("limit", service.execute(configure, "SELECT `key` FROM `test:*` LIMIT 1")).getColumns().size());
        }
    }
}
