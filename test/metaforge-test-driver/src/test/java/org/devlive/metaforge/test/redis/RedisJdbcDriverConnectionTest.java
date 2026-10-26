package org.devlive.metaforge.test.redis;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class RedisJdbcDriverConnectionTest
        extends RedisJdbcBaseTest
{
    @Test
    public void testConnection()
    {
        assertNotNull("Connection should not be null", connection);
    }

    @Test
    public void testConnectionNotClosed()
            throws SQLException
    {
        assertFalse("Connection should not be closed", connection.isClosed());
    }

    @Test
    public void testCreateStatement()
            throws SQLException
    {
        assertNotNull("Statement should not be null", statement);
    }

    @Test
    public void testCloseConnection()
            throws Exception
    {
        Properties props = new Properties();
        props.setProperty("database", "0");

        String jdbcUrl = String.format("jdbc:redis://%s:%d",
                REDIS_CONTAINER.getHost(),
                REDIS_CONTAINER.getFirstMappedPort()
        );

        Connection conn = DriverManager.getConnection(jdbcUrl, props);
        assertFalse("Connection should be open", conn.isClosed());

        conn.close();
        assertTrue("Connection should be closed", conn.isClosed());
    }

    @Test
    public void testAutoCommit()
            throws SQLException
    {
        assertTrue("Auto commit should be true", connection.getAutoCommit());
    }
}
