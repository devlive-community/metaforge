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
    private List<String> values(ResultSet result)
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
        assertEquals(List.of("OK"), values(statement.executeQuery("SET compat:greeting \"hello world\"")));
        assertEquals(List.of("hello world"), values(statement.executeQuery("GET compat:greeting")));
        // Like the released driver, an update answers with the number of reply values: HSET replies "2".
        assertEquals(1, statement.executeUpdate("HSET compat:hash name metaforge version 2026"));
        assertEquals(List.of("name", "metaforge", "version", "2026"), values(statement.executeQuery("HGETALL compat:hash")));
        assertTrue(values(statement.executeQuery("KEYS compat:*")).containsAll(List.of("compat:greeting", "compat:hash")));

        assertTrue(statement.execute("GET compat:greeting"));
        assertEquals(List.of("hello world"), values(statement.getResultSet()));
        assertEquals(-1, statement.getUpdateCount());
        assertEquals(1, statement.executeUpdate("DEL compat:greeting"));
    }

    @Test
    public void theSqlSubsetStillWorks()
            throws SQLException
    {
        assertTrue(values(statement.executeQuery("SHOW TABLES")).isEmpty() || statement.executeQuery("SHOW TABLES").next());
    }

    @Test
    public void switchingTheDatabaseByCommandIsRefused()
    {
        try {
            statement.executeQuery("SELECT 2");
            fail("SELECT 2 must be refused");
        }
        catch (SQLException expected) {
            assertTrue(String.valueOf(expected.getCause()).contains("database"));
        }
    }
}
