package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.h2.tools.Server;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the H2 TCP plugin through the H2 JDBC driver against an H2 TCP server of the version the
 * plugin bundles: schemas, tables and columns created with SQL are listed as databases, tables and
 * columns, and their rows read back. The server is the same library started in process, so the
 * test needs no Docker.
 */
public class H2TCPE2ETest
{
    private static final String BUNDLED = "2.1.214";

    @Test
    public void schemasTablesAndColumnsAreListedAndRead()
            throws Exception
    {
        Path directory = Files.createTempDirectory("metaforge-h2-tcp");
        Server server = Server.createTcpServer("-tcpPort", "0", "-ifNotExists", "-baseDir", directory.toString()).start();
        try {
            PluginService service = new H2TCPService();
            RuntimeConfigure configure = configure("127.0.0.1", server.getPort(), "sa", "", "e2e");

            String reported = first(ok("validator", service.execute(configure, service.validator())));
            Assert.assertTrue(reported + " is not " + BUNDLED, reported.startsWith(BUNDLED));

            ok("create schema", service.execute(configure, "CREATE SCHEMA app"));
            List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
            Assert.assertTrue(databases.toString(), databases.contains("APP"));
            ok("create table", service.execute(configure, "CREATE TABLE app.items (id INT PRIMARY KEY, name VARCHAR(32) NOT NULL)"));
            ok("insert", service.execute(configure, "INSERT INTO app.items VALUES (1, 'a'), (2, 'b')"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, "APP")), "object_name").contains("ITEMS"));
            List<String> columns = column(ok("columns", service.getColumns(configure, "APP", "ITEMS")), "object_name");
            Assert.assertTrue(columns.toString(), columns.containsAll(List.of("ID", "NAME")));
            Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM app.items WHERE id = 2"))));

            ok("drop schema", service.execute(configure, "DROP SCHEMA app CASCADE"));
            Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("APP"));
        }
        finally {
            server.stop();
        }
    }
}
