package org.devlive.metaforge.test.redis;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RedisJdbcDriverSelectTest
        extends RedisJdbcBaseTest
{
    private static final Logger log = LoggerFactory.getLogger(RedisJdbcDriverSelectTest.class);

    @Test
    public void testSelectAllKeys()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test")) {
            assertTrue("Should have at least one row", rs.next());

            String key = rs.getString("key");
            String type = rs.getString("type");
            Object value = rs.getObject("value");

            assertNotNull("Key should not be null", key);
            assertNotNull("Type should not be null", type);
            log.info("Key: {}, Type: {}, Value: {}", key, type, value);

            int count = 1;
            while (rs.next()) {
                log.info("Key: {}, Type: {}, Value: {}",
                        rs.getString("key"),
                        rs.getString("type"),
                        rs.getObject("value"));
                count++;
            }
            log.info("Total keys: {}", count);
        }
    }

    @Test
    public void testSelectByPattern()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test:*")) {
            assertTrue("Should have at least one row", rs.next());

            String key = rs.getString("key");
            assertTrue("Key should start with 'test:'", key.startsWith("test:"));
            log.info("Key: {}, Type: {}, Value: {}",
                    key,
                    rs.getString("type"),
                    rs.getObject("value"));

            int count = 1;
            while (rs.next()) {
                log.info("Key: {}, Type: {}, Value: {}",
                        rs.getString("key"),
                        rs.getString("type"),
                        rs.getObject("value"));
                count++;
            }
            log.info("Total matching keys: {}", count);
        }
    }

    @Test
    public void testSelectStringKey()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test:string")) {
            assertTrue("Should have at least one row", rs.next());

            String key = rs.getString("key");
            String type = rs.getString("type");
            Object value = rs.getObject("value");

            log.info("Key: {}, Type: {}, Value: {}", key, type, value);
            assertTrue("Key should contain 'test:string'", key.contains("test:string"));
        }
    }
}
