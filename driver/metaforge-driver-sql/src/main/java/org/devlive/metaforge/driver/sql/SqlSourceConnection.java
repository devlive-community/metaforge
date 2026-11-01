package org.devlive.metaforge.driver.sql;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.driver.sql.base.BaseConnection;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

/** A connection to a source without SQL: statements run through {@link SqlEngine}; there are no transactions. */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "JDBC objects share their connection, statement and source")
public class SqlSourceConnection
        extends BaseConnection
{
    private final SqlSource source;
    private final SqlEngine engine;
    private final String url;
    private final String user;
    private final Properties clientInfo = new Properties();
    private boolean closed;
    private boolean readOnly;

    public SqlSourceConnection(SqlSource source, String url, String database, String user)
    {
        this.source = source;
        this.engine = new SqlEngine(source, database);
        this.url = url;
        this.user = user;
    }

    public SqlSource getSource()
    {
        return source;
    }

    SqlEngine getEngine()
    {
        return engine;
    }

    String getUrl()
    {
        return url;
    }

    String getUser()
    {
        return user;
    }

    void ensureOpen()
            throws SQLException
    {
        if (closed) {
            throw new SQLException("Connection is closed");
        }
    }

    @Override
    public Statement createStatement()
            throws SQLException
    {
        ensureOpen();
        return new SqlSourceStatement(this);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException
    {
        if (resultSetType != ResultSet.TYPE_FORWARD_ONLY || resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
            throw new SQLException("Only forward-only, read-only result sets are supported");
        }
        return createStatement();
    }

    @Override
    public DatabaseMetaData getMetaData()
            throws SQLException
    {
        ensureOpen();
        return new SqlSourceDatabaseMetaData(this);
    }

    @Override
    public String nativeSQL(String sql)
    {
        return sql;
    }

    @Override
    public boolean getAutoCommit()
    {
        return true;
    }

    /** Every statement applies at once; a request for transactions is refused. */
    @Override
    public void setAutoCommit(boolean autoCommit)
            throws SQLException
    {
        if (!autoCommit) {
            throw new SQLException("Transactions are not supported");
        }
    }

    @Override
    public void commit()
    {
    }

    @Override
    public void rollback()
            throws SQLException
    {
        throw new SQLException("Transactions are not supported");
    }

    @Override
    public int getTransactionIsolation()
    {
        return TRANSACTION_NONE;
    }

    @Override
    public void setTransactionIsolation(int level)
            throws SQLException
    {
        if (level != TRANSACTION_NONE) {
            throw new SQLException("Transactions are not supported");
        }
    }

    @Override
    public String getCatalog()
    {
        return null;
    }

    @Override
    public void setCatalog(String catalog)
    {
    }

    @Override
    public String getSchema()
    {
        return engine.getDatabase();
    }

    @Override
    public void setSchema(String schema)
            throws SQLException
    {
        ensureOpen();
        if (!source.databases().contains(schema)) {
            throw new SQLException("Unknown database " + schema);
        }
        engine.setDatabase(schema);
    }

    @Override
    public boolean isReadOnly()
    {
        return readOnly;
    }

    @Override
    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    @Override
    public int getHoldability()
    {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public void setHoldability(int holdability)
    {
    }

    @Override
    public boolean isValid(int timeout)
    {
        if (closed) {
            return false;
        }
        try {
            source.databases();
            return true;
        }
        catch (SQLException error) {
            return false;
        }
    }

    @Override
    public SQLWarning getWarnings()
    {
        return null;
    }

    @Override
    public void clearWarnings()
    {
    }

    @Override
    public void setClientInfo(String name, String value)
    {
        clientInfo.setProperty(name, value);
    }

    @Override
    public void setClientInfo(Properties properties)
            throws SQLClientInfoException
    {
        clientInfo.putAll(properties);
    }

    @Override
    public String getClientInfo(String name)
    {
        return clientInfo.getProperty(name);
    }

    @Override
    public Properties getClientInfo()
    {
        Properties copy = new Properties();
        copy.putAll(clientInfo);
        return copy;
    }

    @Override
    public Map<String, Class<?>> getTypeMap()
    {
        return Map.of();
    }

    @Override
    public void close()
            throws SQLException
    {
        if (!closed) {
            closed = true;
            source.close();
        }
    }

    @Override
    public boolean isClosed()
    {
        return closed;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
    {
        return iface.isInstance(this) || iface.isInstance(source);
    }

    @Override
    public <T> T unwrap(Class<T> iface)
            throws SQLException
    {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        if (iface.isInstance(source)) {
            return iface.cast(source);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }
}
