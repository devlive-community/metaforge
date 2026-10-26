package org.devlive.metaforge.test.mongo;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

@Slf4j
public class MongoJdbcDriverAggregationTest
        extends MongoJdbcBaseTest
{
    @Test
    public void testOrderBy()
            throws SQLException
    {
        log.info("Test order by");
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test.sample ORDER BY name DESC, value DESC")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testLimit()
            throws SQLException
    {
        log.info("Test limit");
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test.sample LIMIT 1")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testGroupBy()
            throws SQLException
    {
        log.info("Test group by");
        try (ResultSet rs = statement.executeQuery("SELECT name FROM test.sample GROUP BY name")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testGroupByWithLimit()
            throws SQLException
    {
        log.info("Test group by with limit");
        try (ResultSet rs = statement.executeQuery("SELECT name FROM test.sample GROUP BY name LIMIT 1")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testAggregationFunction()
            throws SQLException
    {
        log.info("Test aggregation function");
        try (ResultSet rs = statement.executeQuery("SELECT name, SUM(value) FROM test.sample GROUP BY name")) {
            assertTrue(rs.next());
        }
    }
}
