package org.devlive.metaforge.driver;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

@SuppressFBWarnings(value = {"CT_CONSTRUCTOR_THROW", "NP_NONNULL_RETURN_VIOLATION"})
public class MongoConnection
        implements Connection
{
    @Getter
    private final MongoDatabase database;
    private final MongoClient mongoClient;
    private boolean isClosed = false;

    // Constructor to establish MongoDB connection
    // 构造函数用于建立MongoDB连接
    public MongoConnection(String url, Properties info)
            throws SQLException
    {
        try {
            String databaseName = info.getProperty("database", "admin");

            // 如果URL中包含认证信息，直接使用URL创建客户端
            // If the URL contains authentication information, create a client directly using the URL
            if (url.contains("@")) {
                this.mongoClient = MongoClients.create(url);
            }
            else {
                // 否则检查Properties中的认证信息
                // Otherwise, check the authentication information in Properties
                String username = info.getProperty("user");
                String password = info.getProperty("password");

                if (username != null && password != null) {
                    // 创建认证凭证
                    // Create authentication credentials
                    MongoCredential credential = MongoCredential.createCredential(
                            username,
                            databaseName,
                            password.toCharArray()
                    );

                    // 解析主机和端口
                    // Parse host and port
                    String[] hostPort = url.split("://")[1].split(":");
                    String host = hostPort[0];
                    int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 27017;

                    // 创建带认证的客户端设置
                    // Create client settings with authentication
                    MongoClientSettings settings = MongoClientSettings.builder()
                            .credential(credential)
                            .applyToClusterSettings(builder ->
                                    builder.hosts(Collections.singletonList(new ServerAddress(host, port))))
                            .build();

                    this.mongoClient = MongoClients.create(settings);
                }
                else {
                    // 无认证信息，直接连接
                    // No authentication information, connect directly
                    // Remove jdbc:
                    this.mongoClient = MongoClients.create(url.substring(5));
                }
            }

            this.database = mongoClient.getDatabase(databaseName);

            // 验证连接
            // Verify connection
            database.runCommand(new org.bson.Document("ping", 1));
        }
        catch (Exception e) {
            throw new SQLException("Failed to connect to MongoDB: " + e.getMessage(), e);
        }
    }

    // Create statement for executing queries
    // 创建用于执行查询的Statement
    @Override
    public Statement createStatement()
            throws SQLException
    {
        checkClosed();
        return new MongoStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql)
            throws SQLException
    {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql)
            throws SQLException
    {
        return null;
    }

    @Override
    public String nativeSQL(String sql)
            throws SQLException
    {
        return "";
    }

    // Check if connection is closed
    // 检查连接是否已关闭
    private void checkClosed()
            throws SQLException
    {
        if (isClosed) {
            throw new SQLException("Connection is closed");
        }
    }

    // Close the connection
    // 关闭连接
    @Override
    public void close()
    {
        if (!isClosed) {
            mongoClient.close();
            isClosed = true;
        }
    }

    // Check if connection is closed
    // 检查连接是否已关闭
    @Override
    public boolean isClosed()
            throws SQLException
    {
        return isClosed;
    }

    @Override
    public DatabaseMetaData getMetaData()
            throws SQLException
    {
        throw new SQLFeatureNotSupportedException("Method not supported");
    }

    @Override
    public void setReadOnly(boolean readOnly)
            throws SQLException
    {}

    @Override
    public boolean isReadOnly()
            throws SQLException
    {
        return false;
    }

    @Override
    public void setCatalog(String catalog)
            throws SQLException
    {}

    @Override
    public String getCatalog()
            throws SQLException
    {
        return "";
    }

    @Override
    public void setTransactionIsolation(int level)
            throws SQLException
    {}

    @Override
    public int getTransactionIsolation()
            throws SQLException
    {
        return 0;
    }

    @Override
    public SQLWarning getWarnings()
            throws SQLException
    {
        return null;
    }

    @Override
    public void clearWarnings()
            throws SQLException
    {}

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap()
            throws SQLException
    {
        return Map.of();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map)
            throws SQLException
    {}

    @Override
    public void setHoldability(int holdability)
            throws SQLException
    {}

    @Override
    public int getHoldability()
            throws SQLException
    {
        return 0;
    }

    @Override
    public Savepoint setSavepoint()
            throws SQLException
    {
        return null;
    }

    @Override
    public Savepoint setSavepoint(String name)
            throws SQLException
    {
        return null;
    }

    @Override
    public void rollback(Savepoint savepoint)
            throws SQLException
    {}

    @Override
    public void releaseSavepoint(Savepoint savepoint)
            throws SQLException
    {}

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException
    {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException
    {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException
    {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException
    {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException
    {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException
    {
        return null;
    }

    @Override
    public Clob createClob()
            throws SQLException
    {
        return null;
    }

    @Override
    public Blob createBlob()
            throws SQLException
    {
        return null;
    }

    @Override
    public NClob createNClob()
            throws SQLException
    {
        return null;
    }

    @Override
    public SQLXML createSQLXML()
            throws SQLException
    {
        return null;
    }

    @Override
    public boolean isValid(int timeout)
            throws SQLException
    {
        return false;
    }

    @Override
    public void setClientInfo(String name, String value)
            throws SQLClientInfoException
    {}

    @Override
    public void setClientInfo(Properties properties)
            throws SQLClientInfoException
    {}

    @Override
    public String getClientInfo(String name)
            throws SQLException
    {
        return "";
    }

    @Override
    public Properties getClientInfo()
            throws SQLException
    {
        return null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements)
            throws SQLException
    {
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes)
            throws SQLException
    {
        return null;
    }

    @Override
    public void setSchema(String schema)
            throws SQLException
    {}

    @Override
    public String getSchema()
            throws SQLException
    {
        return "";
    }

    @Override
    public void abort(Executor executor)
            throws SQLException
    {}

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds)
            throws SQLException
    {}

    @Override
    public int getNetworkTimeout()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void setAutoCommit(boolean autoCommit)
            throws SQLException
    {
        // MongoDB doesn't support transactions in the same way as relational databases
        // MongoDB 不支持与关系数据库相同的事务
        throw new UnsupportedOperationException("MongoDB doesn't support transactions in the same way as relational databases");
    }

    @Override
    public boolean getAutoCommit()
            throws SQLException
    {
        return true;
    }

    @Override
    public void commit()
            throws SQLException
    {}

    @Override
    public void rollback()
            throws SQLException
    {}

    @Override
    public <T> T unwrap(Class<T> iface)
            throws SQLException
    {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
            throws SQLException
    {
        return false;
    }

    public MongoClient getClient()
    {
        return mongoClient;
    }
}
