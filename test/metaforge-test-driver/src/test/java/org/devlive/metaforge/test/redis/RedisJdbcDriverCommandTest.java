package org.devlive.metaforge.test.redis;

import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Plain Redis commands run as the released driver ran them, with one RESULTS column. */
public class RedisJdbcDriverCommandTest
        extends RedisJdbcBaseTest
{
    private List<String> values(String sql)
            throws SQLException
    {
        try (ResultSet result = statement.executeQuery(sql)) {
            return read(result);
        }
    }

    private static List<String> read(ResultSet result)
            throws SQLException
    {
        List<String> values = new ArrayList<>();
        while (result.next()) {
            values.add(result.getString("RESULTS"));
        }
        return values;
    }

    @Test
    public void commandsReadAndWrite()
            throws SQLException
    {
        assertEquals(List.of("OK"), values("SET compat:greeting \"hello world\""));
        assertEquals(List.of("hello world"), values("GET compat:greeting"));
        // Like the released driver, an update answers with the number of reply values: HSET replies "2".
        assertEquals(1, statement.executeUpdate("HSET compat:hash name metaforge version 2026"));
        assertEquals(List.of("name", "metaforge", "version", "2026"), values("HGETALL compat:hash"));
        assertTrue(values("KEYS compat:*").containsAll(List.of("compat:greeting", "compat:hash")));

        assertTrue(statement.execute("GET compat:greeting"));
        try (ResultSet result = statement.getResultSet()) {
            assertEquals(List.of("hello world"), read(result));
        }
        assertEquals(-1, statement.getUpdateCount());
        assertEquals(1, statement.executeUpdate("DEL compat:greeting"));
    }

    @Test
    public void theSqlSubsetStillWorks()
            throws SQLException
    {
        try (ResultSet result = statement.executeQuery("SHOW TABLES")) {
            assertEquals(1, result.getMetaData().getColumnCount());
        }
    }

    @Test
    public void switchingTheDatabaseByCommandIsRefused()
    {
        try (ResultSet ignored = statement.executeQuery("SELECT 2")) {
            fail("SELECT 2 must be refused");
        }
        catch (SQLException expected) {
            assertTrue(String.valueOf(expected.getCause()).contains("database"));
        }
    }
}
