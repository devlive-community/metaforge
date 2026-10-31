package org.devlive.metaforge;

import org.devlive.metaforge.driver.sql.ConnectionUrl;
import org.devlive.metaforge.driver.sql.SqlSource;
import org.devlive.metaforge.driver.sql.SqlSourceDriver;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC over HDFS: {@code jdbc:hdfs://namenode:8020[?root=/&user=hdfs]}. A database is a
 * directory under {@code root}, a table a directory inside it, and the table's rows its entries
 * with fixed attribute columns. Options other than the driver's own ({@code root}, {@code user},
 * {@code httpPort}) are Hadoop configuration, such as {@code dfs.client.use.datanode.hostname}.
 */
public class HdfsDriver
        extends SqlSourceDriver
{
    static {
        try {
            DriverManager.registerDriver(new HdfsDriver());
        }
        catch (SQLException error) {
            throw new IllegalStateException("Can not register the HDFS JDBC driver", error);
        }
    }

    @Override
    protected String prefix()
    {
        return "jdbc:hdfs://";
    }

    @Override
    protected SqlSource open(ConnectionUrl url)
            throws SQLException
    {
        return HdfsSource.connect(url);
    }
}
