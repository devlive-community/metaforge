package org.devlive.metaforge;

import org.devlive.metaforge.driver.sql.ConnectionUrl;
import org.devlive.metaforge.driver.sql.SqlSource;
import org.devlive.metaforge.driver.sql.SqlSourceDriver;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC over ZooKeeper: {@code jdbc:zookeeper://host:port[,host:port]/database}. A database is a
 * znode under the root, a table its child and a column the table's child, whose data is the value
 * of the table's single row. Options: {@code sessionTimeout} and {@code connectTimeout} in
 * milliseconds, and {@code user} / {@code password} for digest authentication.
 */
public class ZookeeperDriver
        extends SqlSourceDriver
{
    static {
        try {
            DriverManager.registerDriver(new ZookeeperDriver());
        }
        catch (SQLException error) {
            throw new IllegalStateException("Can not register the ZooKeeper JDBC driver", error);
        }
    }

    @Override
    protected String prefix()
    {
        return "jdbc:zookeeper://";
    }

    @Override
    protected SqlSource open(ConnectionUrl url)
            throws SQLException
    {
        return ZookeeperSource.connect(url);
    }
}
