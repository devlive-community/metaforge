package org.devlive.metaforge.test.mongo;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

@Slf4j
public class MongoJdbcDriverShowTest
        extends MongoJdbcBaseTest
{
    @Test
    public void testShowDatabases()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SHOW DATABASES")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testShowTables()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SHOW TABLES FROM test")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testShowColumns()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SHOW COLUMNS FROM test.sample")) {
            assertTrue(rs.next());
        }
    }
}
