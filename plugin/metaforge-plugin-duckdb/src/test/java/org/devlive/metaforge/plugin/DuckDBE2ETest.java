package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
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
 * Runs the DuckDB plugin through the DuckDB JDBC driver against the engine it bundles, on a
 * database file in a directory the way the plugin's form names one: schemas, tables and columns
 * created with SQL are listed as databases, tables and columns, and their rows read back by later
 * queries. DuckDB runs in process, so the test needs no Docker and verifies the bundled version.
 */
public class DuckDBE2ETest
{
    private static final String BUNDLED = "v1.0.0";

    @Test
    public void schemasTablesAndColumnsAreListedAndRead()
            throws Exception
    {
        Path directory = Files.createTempDirectory("metaforge-duckdb");
        PluginService service = new DuckDBService();
        RuntimeConfigure configure = configure(directory.toString(), 0, null, null, "local");

        String reported = first(ok("validator", service.execute(configure, service.validator())));
        Assert.assertEquals(BUNDLED, reported);

        ok("create schema", service.execute(configure, "CREATE SCHEMA app"));
        List<String> databases = column(ok("databases", service.getDatabases(configure)), "object_name");
        Assert.assertTrue(databases.toString(), databases.contains("app"));
        ok("create table", service.execute(configure, "CREATE TABLE app.items (id INTEGER PRIMARY KEY, name VARCHAR NOT NULL)"));
        ok("insert", service.execute(configure, "INSERT INTO app.items VALUES (1, 'a'), (2, 'b')"));
        Assert.assertTrue(column(ok("tables", service.getTables(configure, "app")), "object_name").contains("items"));
        List<String> columns = column(ok("columns", service.getColumns(configure, "app", "items")), "object_name");
        Assert.assertTrue(columns.toString(), columns.containsAll(List.of("id", "name")));
        Assert.assertEquals("b", first(ok("select", service.execute(configure, "SELECT name FROM app.items WHERE id = 2"))));

        ok("drop schema", service.execute(configure, "DROP SCHEMA app CASCADE"));
        Assert.assertFalse(column(ok("databases", service.getDatabases(configure)), "object_name").contains("app"));
    }
}
