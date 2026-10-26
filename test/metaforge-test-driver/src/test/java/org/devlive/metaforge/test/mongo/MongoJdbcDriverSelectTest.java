package org.devlive.metaforge.test.mongo;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

@Slf4j
public class MongoJdbcDriverSelectTest
        extends MongoJdbcBaseTest
{
    @Test
    public void testSelectAll()
            throws SQLException
    {
        log.info("Test simple select");
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test.sample")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testSelectSpecificColumn()
            throws SQLException
    {
        log.info("Test specific select column");
        try (ResultSet rs = statement.executeQuery("SELECT name FROM test.sample")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testSelectWithAlias()
            throws SQLException
    {
        log.info("Test alias");
        try (ResultSet rs = statement.executeQuery("SELECT name as n, value as v FROM test.sample")) {
            assertTrue(rs.next());
        }
    }
}
