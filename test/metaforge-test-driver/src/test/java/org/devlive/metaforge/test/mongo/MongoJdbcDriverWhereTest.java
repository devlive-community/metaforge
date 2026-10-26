package org.devlive.metaforge.test.mongo;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

@Slf4j
public class MongoJdbcDriverWhereTest
        extends MongoJdbcBaseTest
{
    @Test
    public void testSimpleWhere()
            throws SQLException
    {
        log.info("Test simple where clause");
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test.sample WHERE value = 2")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testWhereWithAnd()
            throws SQLException
    {
        log.info("Test multiple where clause by and");
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test.sample WHERE name = 'test1' AND value = 1")) {
            assertTrue(rs.next());
        }
    }

    @Test
    public void testWhereWithOr()
            throws SQLException
    {
        log.info("Test multiple where clause by or");
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test.sample WHERE name = 'test1' OR name = 'test2'")) {
            assertTrue(rs.next());
        }
    }
}
