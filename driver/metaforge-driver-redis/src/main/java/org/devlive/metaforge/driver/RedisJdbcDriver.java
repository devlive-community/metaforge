package org.devlive.metaforge.driver;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class RedisJdbcDriver
        implements Driver
{
    static {
        try {
            DriverManager.registerDriver(new RedisJdbcDriver());
        }
        catch (SQLException e) {
            throw new RuntimeException("Can't register Redis JDBC Driver", e);
        }
    }

    @Override
    public boolean acceptsURL(String url)
            throws SQLException
    {
        return url != null && url.startsWith("jdbc:redis:");
    }

    @Override
    public Connection connect(String url, Properties info)
            throws SQLException
    {
        if (!acceptsURL(url)) {
            return null;
        }
        return new RedisConnection(url, info);
    }

    @Override
    public int getMajorVersion()
    {
        return 1;
    }

    @Override
    public int getMinorVersion()
    {
        return 0;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
            throws SQLException
    {
        return new DriverPropertyInfo[0];
    }

    @Override
    public boolean jdbcCompliant()
    {
        return false;
    }

    @Override
    public Logger getParentLogger()
            throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException("Parent logger is not supported");
    }
}
