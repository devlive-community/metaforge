package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.kafka.KafkaContainer;

import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the Kafka plugin through MetaForge's Kafka JDBC driver against every Kafka release line it
 * documents as verified: topics created, written, read and dropped as tables with SQL.
 */
@RunWith(Parameterized.class)
public class KafkaE2ETest
{
    @Parameterized.Parameters(name = "Kafka {0}")
    public static List<String> versions()
    {
        return List.of("3.8.1", "3.9.1", "4.0.0");
    }

    private final String version;

    public KafkaE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void topicsAreManagedAsTablesOfRecords()
    {
        try (KafkaContainer container = new KafkaContainer("apache/kafka:" + version)) {
            container.start();
            PluginService service = new KafkaService();
            RuntimeConfigure configure = configure(container.getBootstrapServers(), 0, null, null, null);
            configure.setPort(null);

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertFalse(reported, reported.isEmpty());
            if (version.startsWith("3.")) {
                Assert.assertTrue(reported + " is not " + version, reported.startsWith(version.substring(0, 3)));
            }
            Assert.assertEquals(List.of("kafka"), column(ok("databases", service.getDatabases(configure)), "object_name"));

            ok("create table", service.execute(configure, "CREATE TABLE kafka.events (`key` VARCHAR, `value` VARCHAR) WITH ('partitions' = '2', 'retention.ms' = '600000')"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "kafka")), "object_name").contains("events"));
            Assert.assertEquals(List.of("partition", "offset", "timestamp", "key", "value"),
                    column(ok("columns", service.getColumns(configure, "kafka", "events")), "object_name").subList(0, 5));

            ok("insert", service.execute(configure, "INSERT INTO kafka.events (`key`, `value`, `partition`) VALUES ('k1', 'hello', 0), ('k2', 'world', 1), ('k3', 'again', 1)"));
            Response all = ok("select", service.execute(configure, "SELECT `partition`, `offset`, `key`, `value` FROM kafka.events ORDER BY `key`"));
            Assert.assertEquals(List.of("k1", "k2", "k3"), column(all, "key"));
            Assert.assertEquals(List.of("hello"), column(ok("filter", service.execute(configure, "SELECT `value` FROM kafka.events WHERE `key` = 'k1'")), "value"));
            Assert.assertEquals(List.of("1", "1"), column(ok("partition", service.execute(configure, "SELECT `partition` FROM kafka.events WHERE `partition` = 1")), "partition"));
            Assert.assertEquals(1, ok("limit", service.execute(configure, "SELECT * FROM kafka.events LIMIT 1")).getColumns().size());

            Assert.assertFalse("columns of a topic are fixed", Boolean.TRUE.equals(service.execute(configure, "ALTER TABLE kafka.events ADD COLUMN extra VARCHAR").getIsSuccessful()));

            ok("drop table", service.execute(configure, "DROP TABLE kafka.events"));
            Assert.assertFalse(column(ok("tables", service.getTables(configure, "kafka")), "object_name").contains("events"));
        }
    }
}
