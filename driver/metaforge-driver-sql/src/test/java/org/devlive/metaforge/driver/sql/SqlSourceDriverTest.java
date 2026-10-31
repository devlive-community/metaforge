package org.devlive.metaforge.driver.sql;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SqlSourceDriverTest
{
    private final MemorySource source = new MemorySource();
    private Connection connection;

    @Before
    public void connect()
            throws SQLException
    {
        SqlSourceDriver driver = new SqlSourceDriver()
        {
            @Override
            protected String prefix()
            {
                return "jdbc:memory://";
            }

            @Override
            protected SqlSource open(ConnectionUrl url)
            {
                return source;
            }
        };
        Assert.assertTrue(driver.acceptsURL("jdbc:memory://localhost:1"));
        Assert.assertNull(driver.connect("jdbc:mysql://localhost", new Properties()));
        connection = driver.connect("jdbc:memory://localhost:1", new Properties());
    }

    private List<List<String>> query(String sql)
            throws SQLException
    {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            List<List<String>> rows = new ArrayList<>();
            int count = result.getMetaData().getColumnCount();
            List<String> labels = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                labels.add(result.getMetaData().getColumnLabel(i));
            }
            rows.add(labels);
            while (result.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= count; i++) {
                    row.add(result.getString(i));
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private int update(String sql)
            throws SQLException
    {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    @Test
    public void databasesTablesAndColumnsAreManagedWithSql()
            throws SQLException
    {
        update("CREATE DATABASE app");
        update("CREATE DATABASE IF NOT EXISTS app");
        Assert.assertEquals(List.of(List.of("Database"), List.of("app")), query("SHOW DATABASES"));
        update("CREATE TABLE app.users (id BIGINT NOT NULL, name VARCHAR(32), PRIMARY KEY (id))");
        Assert.assertEquals(List.of("Table"), query("SHOW TABLES FROM app").get(0));
        Assert.assertEquals(Arrays.asList("id", "BIGINT", "NO", "PRI", null, ""), query("SHOW COLUMNS FROM users FROM app").get(1));
        update("ALTER TABLE `app`.`users` ADD COLUMN age INT, RENAME COLUMN name TO title");
        update("ALTER TABLE app.users DROP COLUMN age");
        Assert.assertEquals(List.of("id", "title"), query("SELECT * FROM app.users").get(0));
        try {
            update("ALTER TABLE app.users DROP COLUMN missing");
            Assert.fail("unknown column");
        }
        catch (SQLException error) {
            Assert.assertTrue(error.getMessage(), error.getMessage().contains("Unknown column missing"));
        }
        Assert.assertEquals(1, update("DROP TABLE app.users"));
        Assert.assertEquals(0, update("DROP TABLE IF EXISTS app.users"));
        Assert.assertEquals(1, update("DROP DATABASE app"));
        Assert.assertTrue(query("SHOW DATABASES").size() == 1);
    }

    @Test
    public void rowsAreFilteredOrderedLimitedAndChanged()
            throws SQLException
    {
        update("CREATE DATABASE app");
        update("USE app");
        update("CREATE TABLE users (id INT, name VARCHAR)");
        Assert.assertEquals(3, update("INSERT INTO users (id, name) VALUES (1, 'ann'), (2, 'bob'), (3, 'it''s')"));
        Assert.assertEquals(List.of(List.of("who"), List.of("bob"), List.of("ann")), query("SELECT name AS who FROM users WHERE id < 3 ORDER BY id DESC"));
        Assert.assertEquals(List.of(List.of("id", "name"), List.of("2", "bob")), query("SELECT * FROM users LIMIT 1 OFFSET 1"));
        Assert.assertEquals(List.of("3", "it's"), query("SELECT * FROM users WHERE name LIKE 'it%' OR id IN (9)").get(1));
        Assert.assertEquals(List.of(List.of("id"), List.of("3")), query("SELECT id FROM users WHERE NOT name = 'ann' AND id IS NOT NULL LIMIT 1, 5"));
        Assert.assertEquals(1, update("UPDATE users SET name = 'zed' WHERE id = 1"));
        Assert.assertEquals("zed", query("SELECT name FROM users WHERE id = 1").get(1).get(0));
        Assert.assertEquals(2, update("DELETE FROM users WHERE id >= 2"));
        Assert.assertEquals(1, update("INSERT INTO users VALUES (4, 'dan'); INSERT INTO users VALUES (5, 'eve')") - 1);
    }

    @Test
    public void connectionChecksAndMetadataWorkWithoutTables()
            throws SQLException
    {
        Assert.assertEquals(List.of(List.of("version"), List.of("1")), query("SELECT 1 AS version"));
        Assert.assertEquals("1.2.3", query("SELECT version() AS version").get(1).get(0));
        update("CREATE DATABASE app");
        update("CREATE TABLE app.users (id BIGINT PRIMARY KEY, name VARCHAR)");
        DatabaseMetaData meta = connection.getMetaData();
        Assert.assertFalse(meta.supportsTransactions());
        Assert.assertEquals(1, meta.getDatabaseMajorVersion());
        try (ResultSet schemas = meta.getSchemas()) {
            Assert.assertTrue(schemas.next());
            Assert.assertEquals("app", schemas.getString("TABLE_SCHEM"));
        }
        try (ResultSet tables = meta.getTables(null, "app", "%", new String[] {"TABLE"})) {
            Assert.assertTrue(tables.next());
            Assert.assertEquals("users", tables.getString("TABLE_NAME"));
        }
        try (ResultSet columns = meta.getColumns(null, "app", "users", "%")) {
            Assert.assertTrue(columns.next());
            Assert.assertEquals("id", columns.getString("COLUMN_NAME"));
            Assert.assertEquals(DatabaseMetaData.columnNoNulls, columns.getInt("NULLABLE"));
            Assert.assertEquals(1, columns.getInt("ORDINAL_POSITION"));
        }
        try (ResultSet keys = meta.getPrimaryKeys(null, "app", "users")) {
            Assert.assertTrue(keys.next());
            Assert.assertEquals("id", keys.getString("COLUMN_NAME"));
        }
        try (Statement statement = connection.createStatement()) {
            Assert.assertFalse(statement.execute("CREATE TABLE app.more (id INT)"));
            Assert.assertEquals(0, statement.getUpdateCount());
            Assert.assertTrue(statement.execute("SHOW TABLES FROM app"));
        }
    }

    @Test
    public void unsupportedSqlIsRefusedByName()
    {
        try {
            update("CREATE TABLE nowhere (id INT)");
            Assert.fail("no database");
        }
        catch (SQLException error) {
            Assert.assertTrue(error.getMessage(), error.getMessage().contains("No database selected"));
        }
        Assert.assertThrows(java.sql.SQLSyntaxErrorException.class, () -> query("SELECT count(*) FROM t"));
    }
}
