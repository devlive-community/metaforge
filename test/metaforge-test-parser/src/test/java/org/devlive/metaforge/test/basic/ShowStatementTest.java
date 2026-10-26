package org.devlive.metaforge.test.basic;

import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.ShowStatement;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ShowStatementTest
{
    private ShowStatement parse(String sql)
    {
        SQLStatement stmt = SQLParser.parse(sql);

        assertTrue(stmt instanceof ShowStatement);
        return (ShowStatement) stmt;
    }

    @Test
    public void testShowDatabases()
    {
        // Basic SHOW DATABASES
        ShowStatement stmt = parse("SHOW DATABASES");
        assertEquals(ShowStatement.ShowType.DATABASES, stmt.getShowType());
        assertNull(stmt.getPattern());
        assertNull(stmt.getWhereCondition());

        // SHOW DATABASES with LIKE pattern
        stmt = parse("SHOW DATABASES LIKE '%test%'");
        assertEquals(ShowStatement.ShowType.DATABASES, stmt.getShowType());
        assertEquals("%test%", stmt.getPattern());
        assertNull(stmt.getWhereCondition());

        // Case insensitivity test
        stmt = parse("show DATABASES like '%TEST%'");
        assertEquals(ShowStatement.ShowType.DATABASES, stmt.getShowType());
        assertEquals("%TEST%", stmt.getPattern());
    }

    @Test
    public void testShowTables()
    {
        // Basic SHOW TABLES
        ShowStatement stmt = parse("SHOW TABLES");
        assertEquals(ShowStatement.ShowType.TABLES, stmt.getShowType());
        assertNull(stmt.getDatabaseName());
        assertNull(stmt.getPattern());
        assertNull(stmt.getWhereCondition());

        // SHOW TABLES with database
        stmt = parse("SHOW TABLES FROM mydb");
        assertEquals(ShowStatement.ShowType.TABLES, stmt.getShowType());
        assertEquals("mydb", stmt.getDatabaseName());

        // SHOW TABLES with IN keyword
        stmt = parse("SHOW TABLES IN mydb");
        assertEquals(ShowStatement.ShowType.TABLES, stmt.getShowType());
        assertEquals("mydb", stmt.getDatabaseName());

        // SHOW TABLES with LIKE pattern
        stmt = parse("SHOW TABLES FROM mydb LIKE '%user%'");
        assertEquals(ShowStatement.ShowType.TABLES, stmt.getShowType());
        assertEquals("mydb", stmt.getDatabaseName());
        assertEquals("%user%", stmt.getPattern());

        // SHOW TABLES with WHERE clause
        stmt = parse("SHOW TABLES FROM mydb WHERE Tables_in_mydb LIKE '%user%'");
        assertEquals(ShowStatement.ShowType.TABLES, stmt.getShowType());
        assertEquals("mydb", stmt.getDatabaseName());
        assertNotNull(stmt.getWhereCondition());
    }

    @Test
    public void testShowColumns()
    {
        // Basic SHOW COLUMNS
        ShowStatement stmt = parse("SHOW COLUMNS FROM users");
        assertEquals(ShowStatement.ShowType.COLUMNS, stmt.getShowType());
        assertEquals("users", stmt.getTableName());
        assertNull(stmt.getDatabaseName());
        assertNull(stmt.getPattern());

        // SHOW COLUMNS with database
        stmt = parse("SHOW COLUMNS FROM users FROM mydb");
        assertEquals(ShowStatement.ShowType.COLUMNS, stmt.getShowType());
        assertEquals("users", stmt.getTableName());
        assertEquals("mydb", stmt.getDatabaseName());

        // Alternative syntax with IN
        stmt = parse("SHOW COLUMNS FROM users IN mydb");
        assertEquals(ShowStatement.ShowType.COLUMNS, stmt.getShowType());
        assertEquals("users", stmt.getTableName());
        assertEquals("mydb", stmt.getDatabaseName());

        // SHOW COLUMNS with LIKE pattern
        stmt = parse("SHOW COLUMNS FROM users LIKE '%id%'");
        assertEquals(ShowStatement.ShowType.COLUMNS, stmt.getShowType());
        assertEquals("users", stmt.getTableName());
        assertEquals("%id%", stmt.getPattern());

        // SHOW COLUMNS with WHERE clause
        stmt = parse("SHOW COLUMNS FROM users WHERE Field LIKE '%id%'");
        assertEquals(ShowStatement.ShowType.COLUMNS, stmt.getShowType());
        assertEquals("users", stmt.getTableName());
        assertNotNull(stmt.getWhereCondition());

        // Full syntax test
        stmt = parse("SHOW COLUMNS FROM users IN mydb WHERE Field LIKE '%id%'");
        assertEquals(ShowStatement.ShowType.COLUMNS, stmt.getShowType());
        assertEquals("users", stmt.getTableName());
        assertEquals("mydb", stmt.getDatabaseName());
        assertNotNull(stmt.getWhereCondition());
    }

    @Test
    public void testEdgeCases()
    {
        // Mixed case
        ShowStatement stmt = parse("ShOw DaTaBaSeS LiKe '%test%'");
        assertEquals(ShowStatement.ShowType.DATABASES, stmt.getShowType());
        assertEquals("%test%", stmt.getPattern());

        // Extra whitespace
        stmt = parse("SHOW   TABLES    FROM    mydb");
        assertEquals(ShowStatement.ShowType.TABLES, stmt.getShowType());
        assertEquals("mydb", stmt.getDatabaseName());

        // Complex WHERE conditions
        stmt = parse("SHOW COLUMNS FROM users WHERE Field LIKE '%id%' AND Type = 'int'");
        assertEquals(ShowStatement.ShowType.COLUMNS, stmt.getShowType());
        assertEquals("users", stmt.getTableName());
        assertNotNull(stmt.getWhereCondition());
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidSyntax()
    {
        // This should throw an exception
        parse("SHOW INVALID");
    }

    @Test
    public void testQuotedIdentifiers()
    {
        // Test with quoted database name
        ShowStatement stmt = parse("SHOW TABLES FROM `my-db`");
        assertEquals(ShowStatement.ShowType.TABLES, stmt.getShowType());
        assertEquals("`my-db`", stmt.getDatabaseName());

        // Test with quoted table name
        stmt = parse("SHOW COLUMNS FROM `user-table`");
        assertEquals(ShowStatement.ShowType.COLUMNS, stmt.getShowType());
        assertEquals("`user-table`", stmt.getTableName());

        // Test with both quoted
        stmt = parse("SHOW COLUMNS FROM `user-table` IN `my-db`");
        assertEquals(ShowStatement.ShowType.COLUMNS, stmt.getShowType());
        assertEquals("`user-table`", stmt.getTableName());
        assertEquals("`my-db`", stmt.getDatabaseName());
    }

    @Test
    public void testPatternQuotes()
    {
        // Test with single quotes
        ShowStatement stmt = parse("SHOW DATABASES LIKE '%test%'");
        assertEquals("%test%", stmt.getPattern());

        // Test with double quotes
        stmt = parse("SHOW DATABASES LIKE \"%test%\"");
        assertEquals("%test%", stmt.getPattern());
    }
}
