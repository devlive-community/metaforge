package org.devlive.metaforge.driver.sql;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * A JDBC driver for a source without SQL. Subclasses name their URL prefix and open the source;
 * the rest of JDBC comes from this package.
 */
public abstract class SqlSourceDriver
        implements Driver
{
    /** The URL prefix up to the hosts, such as {@code jdbc:zookeeper://}. */
    protected abstract String prefix();

    protected abstract SqlSource open(ConnectionUrl url)
            throws SQLException;

    @Override
    public boolean acceptsURL(String url)
    {
        return url != null && url.startsWith(prefix());
    }

    @Override
    public Connection connect(String url, Properties info)
            throws SQLException
    {
        if (!acceptsURL(url)) {
            return null;
        }
        ConnectionUrl parsed = ConnectionUrl.parse(prefix(), url, info);
        SqlSource source = open(parsed);
        String database = parsed.getDatabase();
        if (!database.isEmpty() && !source.databases().contains(database)) {
            source.close();
            throw new SQLException("Unknown database " + database);
        }
        return new SqlSourceConnection(source, url, database, parsed.get("user", null));
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
    {
        return new DriverPropertyInfo[0];
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
