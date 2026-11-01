package org.devlive.metaforge;

import org.devlive.metaforge.driver.sql.ConnectionUrl;
import org.devlive.metaforge.driver.sql.SqlSource;
import org.devlive.metaforge.driver.sql.SqlSourceDriver;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC over Aliyun OSS: {@code jdbc:alioss://oss-cn-hangzhou.aliyuncs.com[/bucket][?ssl=true]}
 * with the AccessKey ID as user and the AccessKey secret as password. A bucket is a database, a
 * top-level prefix ({@code logs/}) a table and the objects under it its rows. Options:
 * {@code ssl} (default true), {@code maxRows} (default 10000) and {@code contentLimit}, the
 * largest object in bytes whose text SELECT reads into {@code content} (default 0, not read).
 */
public class AliOssDriver
        extends SqlSourceDriver
{
    static {
        try {
            DriverManager.registerDriver(new AliOssDriver());
        }
        catch (SQLException error) {
            throw new IllegalStateException("Can not register the Aliyun OSS JDBC driver", error);
        }
    }

    @Override
    protected String prefix()
    {
        return "jdbc:alioss://";
    }

    @Override
    protected SqlSource open(ConnectionUrl url)
            throws SQLException
    {
        return AliOssSource.connect(url);
    }
}
