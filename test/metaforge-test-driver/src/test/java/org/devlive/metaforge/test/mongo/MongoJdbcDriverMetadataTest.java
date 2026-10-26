package org.devlive.metaforge.test.mongo;

import org.junit.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class MongoJdbcDriverMetadataTest
        extends MongoJdbcBaseTest
{
    @Test
    public void testSelect()
            throws SQLException
    {
        try (ResultSet rs = statement.executeQuery("SELECT * FROM test.sample")) {
            ResultSetMetaData metaData = rs.getMetaData();

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                System.out.println("Column " + i + ":");
                System.out.println("  Name: " + metaData.getColumnName(i));
                System.out.println("  Type: " + metaData.getColumnTypeName(i));
                System.out.println("  SQL Type: " + metaData.getColumnType(i));
                System.out.println("  Java Class: " + metaData.getColumnClassName(i));
            }
        }
    }
}
