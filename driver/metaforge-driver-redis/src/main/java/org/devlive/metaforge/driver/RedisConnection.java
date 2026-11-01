package org.devlive.metaforge.driver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

@SuppressFBWarnings(value = {"CT_CONSTRUCTOR_THROW", "NP_NONNULL_RETURN_VIOLATION"})
public class RedisConnection
        implements Connection
{
    static final String PREFIX = "jdbc:redis:";
    static final String CLUSTER_PREFIX = "jdbc:redis-cluster:";

    private final RedissonClient client;
    private final int database;
    private boolean isClosed = false;

    public RedisConnection(String url, Properties info)
            throws SQLException
    {
        try {
            String username = info.getProperty("user");
            String password = info.getProperty("password");
            Config config = new Config();
            if (isCluster(url)) {
                // jdbc:redis-cluster://host1:port1,host2:port2 as the released cluster driver took it
                this.database = 0;
                ClusterServersConfig cluster = config.useClusterServers().addNodeAddress(addresses(url).toArray(new String[0]));
                if (password != null) {
                    cluster.setPassword(password);
                }
                if (username != null && password != null) {
                    cluster.setUsername(username);
                }
            }
            else {
                this.database = database(url, info);
                SingleServerConfig single = config.useSingleServer().setAddress(addresses(url).get(0)).setDatabase(database);
                if (password != null) {
                    single.setPassword(password);
                }
                if (username != null && password != null) {
                    single.setUsername(username);
                }
            }

            this.client = Redisson.create(config);
            client.getKeys().count();
        }
        catch (Exception e) {
            throw new SQLException("Failed to connect to Redis: " + e.getMessage(), e);
        }
    }

    static boolean isCluster(String url)
    {
        return url.startsWith(CLUSTER_PREFIX);
    }

    /** The node addresses of jdbc:redis://host:port[/db] or jdbc:redis-cluster://host:port,host:port. */
    static List<String> addresses(String url)
    {
        String rest = location(url);
        int path = rest.indexOf('/');
        if (path >= 0) {
            rest = rest.substring(0, path);
        }
        List<String> addresses = new ArrayList<>();
        for (String node : rest.split(",")) {
            String trimmed = node.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            addresses.add("redis://" + (trimmed.contains(":") ? trimmed : trimmed + ":6379"));
        }
        if (addresses.isEmpty()) {
            addresses.add("redis://localhost:6379");
        }
        return addresses;
    }

    /** The database property, else the URL path (jdbc:redis://host:port/2), else 0. */
    static int database(String url, Properties info)
    {
        String property = info.getProperty("database");
        if (property != null && !property.trim().isEmpty()) {
            return Integer.parseInt(property.trim());
        }
        String rest = location(url);
        int path = rest.indexOf('/');
        String value = path >= 0 ? rest.substring(path + 1).trim() : "";
        return value.matches("\\d+") ? Integer.parseInt(value) : 0;
    }

    private static String location(String url)
    {
        String rest = url.substring(isCluster(url) ? CLUSTER_PREFIX.length() : PREFIX.length());
        while (rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        int query = rest.indexOf('?');
        return query >= 0 ? rest.substring(0, query) : rest;
    }

    public RedissonClient getClient()
    {
        return client;
    }

    public int getDatabase()
    {
        return database;
    }

    @Override
    public Statement createStatement()
            throws SQLException
    {
        checkClosed();
        return new RedisStatement(this);
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

    private void checkClosed()
            throws SQLException
    {
        if (isClosed) {
            throw new SQLException("Connection is closed");
        }
    }

    @Override
    public void close()
    {
        if (!isClosed) {
            client.shutdown();
            isClosed = true;
        }
    }

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
        throw new UnsupportedOperationException("Redis doesn't support transactions in the same way as relational databases");
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
}
