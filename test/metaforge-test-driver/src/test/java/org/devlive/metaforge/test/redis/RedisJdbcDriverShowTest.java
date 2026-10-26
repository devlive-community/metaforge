package org.devlive.metaforge.test.redis;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RedisJdbcDriverShowTest
        extends RedisJdbcBaseTest
{
    private static final Logger log = LoggerFactory.getLogger(RedisJdbcDriverShowTest.class);

    @Test
    public void testShowDatabases()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SHOW DATABASES")) {
            assertTrue("Should have at least one database", rs.next());
            String name = rs.getString("name");
            assertNotNull("Database name should not be null", name);
            log.info("Database: {}", name);

            int count = 1;
            while (rs.next()) {
                count++;
            }
            log.info("Total databases: {}", count);
        }
    }

    @Test
    public void testShowTables()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SHOW TABLES")) {
            assertTrue("Should have at least one key", rs.next());
            String name = rs.getString("name");
            assertNotNull("Key name should not be null", name);
            log.info("Key: {}", name);

            int count = 1;
            while (rs.next()) {
                log.info("Key: {}", rs.getString("name"));
                count++;
            }
            log.info("Total keys: {}", count);
        }
    }

    @Test
    public void testShowColumns()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SHOW COLUMNS FROM test:hash")) {
            assertTrue("Should have at least one field", rs.next());
            String name = rs.getString("name");
            assertNotNull("Field name should not be null", name);
            log.info("Field: {}", name);

            int count = 1;
            while (rs.next()) {
                log.info("Field: {}", rs.getString("name"));
                count++;
            }
            log.info("Total fields: {}", count);
        }
    }
}
