package org.devlive.metaforge.driver;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class MongoJdbcDriver
        implements Driver
{
    // Static initialization of driver
    // 静态初始化驱动
    static {
        try {
            DriverManager.registerDriver(new MongoJdbcDriver());
        }
        catch (SQLException e) {
            throw new RuntimeException("Can't register MongoDB JDBC Driver", e);
        }
    }

    // Check if this driver can handle the given URL
    // 检查驱动是否可以处理给定的URL
    @Override
    public boolean acceptsURL(String url)
            throws SQLException
    {
        return url != null && (
                url.startsWith("jdbc:mongo:")
                        || url.startsWith("jdbc:mongodb:")
                        || url.startsWith("jdbc:mongodb+srv:")
        );
    }

    // Connect to MongoDB database
    // 连接MongoDB数据库
    @Override
    public Connection connect(String url, Properties info)
            throws SQLException
    {
        if (!acceptsURL(url)) {
            return null;
        }

        return new MongoConnection(url, info);
    }

    // Get driver's major version
    // 获取驱动主版本号
    @Override
    public int getMajorVersion()
    {
        return 1;
    }

    // Get driver's minor version
    // 获取驱动次版本号
    @Override
    public int getMinorVersion()
    {
        return 0;
    }

    // Get driver's property info
    // 获取驱动属性信息
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
            throws SQLException
    {
        return new DriverPropertyInfo[0];
    }

    // Check if driver is JDBC compliant
    // 检查驱动是否符合JDBC规范
    @Override
    public boolean jdbcCompliant()
    {
        return false;
    }

    // Get parent logger
    // 获取父日志记录器
    @Override
    public Logger getParentLogger()
            throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException("Parent logger is not supported");
    }
}
