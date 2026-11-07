package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.first;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the H2 plugin through the H2 JDBC driver against the in-memory engine it bundles, with the
 * host and port the plugin's form fixes: schemas, tables and columns created with SQL are listed
 * as databases, tables and columns, and their rows read back by later queries. An embedded engine
 * has no container to start, so the test needs no Docker and verifies the bundled version.
 */
public class H2E2ETest
{
    private static final String BUNDLED = "2.1.214";

    @Test
    public void schemasTablesAndColumnsAreListedAndRead()
    {
        PluginService service = new H2Service();
        RuntimeConfigure configure = configure("-", 1, "sa", null, "e2e");

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
}
