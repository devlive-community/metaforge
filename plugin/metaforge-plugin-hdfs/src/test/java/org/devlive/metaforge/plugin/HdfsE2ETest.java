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
import java.util.Map;
import java.util.Optional;

import static org.devlive.metaforge.test.e2e.PluginE2E.assumeDocker;
import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the HDFS plugin through MetaForge's HDFS JDBC driver against every Hadoop release line it
 * documents as verified: directories created, listed, changed and dropped as databases, tables
 * and rows with SQL. The NameNode alone serves everything the driver does.
 */
@RunWith(Parameterized.class)
public class HdfsE2ETest
{
    @Parameterized.Parameters(name = "Hadoop {0}")
    public static List<String> versions()
    {
        return List.of("3.3.6", "3.4.1");
    }

    private final String version;

    public HdfsE2ETest(String version)
    {
        this.version = version;
    }

    @BeforeClass
    public static void docker()
    {
        assumeDocker();
    }

    @Test
    public void directoriesAreManagedAsDatabasesTablesAndRows()
    {
        try (GenericContainer<?> container = new GenericContainer<>("apache/hadoop:" + version)
                .withCommand("hdfs", "namenode")
                .withEnv("ENSURE_NAMENODE_DIR", "/tmp/hadoop-hadoop/dfs/name")
                .withEnv("CORE-SITE.XML_fs.defaultFS", "hdfs://localhost:8020")
                .withEnv("HDFS-SITE.XML_dfs.namenode.rpc-bind-host", "0.0.0.0")
                .withEnv("HDFS-SITE.XML_dfs.namenode.http-bind-host", "0.0.0.0")
                .withEnv("HDFS-SITE.XML_dfs.namenode.safemode.extension", "0")
                .withEnv("HDFS-SITE.XML_dfs.replication", "1")
                .withExposedPorts(8020, 9870)
                .waitingFor(Wait.forHttp("/jmx?qry=Hadoop:service=NameNode,name=NameNodeStatus").forPort(9870)
                        .forResponsePredicate(body -> body.contains("\"State\" : \"active\""))
                        .withStartupTimeout(Duration.ofMinutes(5)))) {
            container.start();
            PluginService service = new HdfsService();
            RuntimeConfigure configure = configure(container.getHost(), container.getMappedPort(8020), "hadoop", null, null);
            configure.setEnv(Optional.of(Map.of("httpPort", container.getMappedPort(9870))));

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + version, reported.startsWith(version));

            ok("create database", service.execute(configure, "CREATE DATABASE warehouse"));
            Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains("warehouse"));
            ok("create table", service.execute(configure, "CREATE TABLE warehouse.logs (name VARCHAR)"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "warehouse")), "object_name").contains("logs"));
            Assert.assertTrue(column(ok("columns", service.getColumns(configure, "warehouse", "logs")), "object_name").containsAll(List.of("name", "type", "size", "permission")));

            ok("insert", service.execute(configure, "INSERT INTO warehouse.logs (name, type) VALUES ('a.log', 'file'), ('archive', 'directory')"));
            Assert.assertEquals(List.of("a.log"), column(ok("files", service.execute(configure, "SELECT name, size FROM warehouse.logs WHERE type = 'file'")), "name"));
            ok("rename", service.execute(configure, "UPDATE warehouse.logs SET name = 'b.log' WHERE name = 'a.log'"));
            ok("permission", service.execute(configure, "UPDATE warehouse.logs SET permission = '600' WHERE name = 'b.log'"));
            Assert.assertEquals(List.of("rw-------"), column(ok("changed", service.execute(configure, "SELECT permission FROM warehouse.logs WHERE name = 'b.log'")), "permission"));
            ok("delete", service.execute(configure, "DELETE FROM warehouse.logs WHERE type = 'directory'"));
            Assert.assertEquals(List.of("b.log"), column(ok("left", service.execute(configure, "SELECT name FROM warehouse.logs")), "name"));

            Assert.assertFalse("columns of a directory are fixed", Boolean.TRUE.equals(service.execute(configure, "ALTER TABLE warehouse.logs ADD COLUMN extra VARCHAR").getIsSuccessful()));

            ok("drop table", service.execute(configure, "DROP TABLE warehouse.logs"));
            Assert.assertFalse(column(ok("tables", service.getTables(configure, "warehouse")), "object_name").contains("logs"));
            ok("drop database", service.execute(configure, "DROP DATABASE warehouse"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("warehouse"));
        }
    }
}
