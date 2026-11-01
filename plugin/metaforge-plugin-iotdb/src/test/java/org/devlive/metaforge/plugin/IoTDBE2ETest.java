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

import java.time.Duration;
import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the IoTDB plugin through the IoTDB JDBC driver against every IoTDB release it documents as
 * verified: a database and time series created with SQL are listed as a database, devices and
 * measurements, and their points read back.
 */
@RunWith(Parameterized.class)
public class IoTDBE2ETest
{
    @Parameterized.Parameters(name = "IoTDB {0}")
    public static List<String> versions()
    {
        return List.of("1.3.3", "2.0.5");
    }

    private final String version;

    public IoTDBE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void databasesDevicesAndMeasurementsAreListedAndRead()
    {
        try (GenericContainer<?> container = new GenericContainer<>("apache/iotdb:" + version + "-standalone")
                .withExposedPorts(6667)
                .waitingFor(Wait.forLogMessage(".*Congratulations.*IoTDB.*", 1).withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            PluginService service = new IoTDBService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(6667), "root", "root", null);
            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version));

            ok("create database", service.execute(configure, "CREATE DATABASE root.e2e"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("root.e2e"));
            ok("insert", service.execute(configure, "INSERT INTO root.e2e.d1(timestamp, reading) VALUES (1700000000000, 1.5)"));
            ok("insert", service.execute(configure, "INSERT INTO root.e2e.d1(timestamp, reading) VALUES (1700000001000, 2.5)"));
            List<String> tables = column(ok("tables", service.getTables(configure, "root.e2e")), "object_name");
            Assert.assertTrue(tables.toString(), tables.contains("root.e2e.d1"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "root.e2e", "root.e2e.d1")), "object_name");
            Assert.assertTrue(columns.toString(), columns.contains("reading"));
            Assert.assertEquals("2", first(ok("count", service.execute(configure, "SELECT COUNT(reading) FROM root.e2e.d1"))));

            ok("drop database", service.execute(configure, "DELETE DATABASE root.e2e"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("root.e2e"));
        }
    }
}
