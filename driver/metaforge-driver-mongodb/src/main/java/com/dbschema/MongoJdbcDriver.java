package com.dbschema;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.DriverManager;
import java.sql.SQLException;

@SuppressFBWarnings(value = {"NM_SAME_SIMPLE_NAME_AS_SUPERCLASS"})
public class MongoJdbcDriver
        extends org.devlive.metaforge.driver.MongoJdbcDriver
{
    static {
        try {
            DriverManager.registerDriver(new MongoJdbcDriver());
        }
        catch (SQLException e) {
            throw new RuntimeException("Can't register com.dbschema.MongoJdbcDriver", e);
        }
    }
}
