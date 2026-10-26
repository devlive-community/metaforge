package org.devlive.metaforge.test.mongo;

import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

public class MongoJdbcDriverVersionTest
        extends MongoJdbcBaseTest
{
    @Test
    public void test()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SELECT version()")) {
            assertTrue(rs.next());
        }
    }
}
