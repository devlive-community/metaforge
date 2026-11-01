package org.devlive.metaforge.driver.sql;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.driver.sql.base.BaseStatement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "JDBC objects share their connection, statement and source")
public class SqlSourceStatement
        extends BaseStatement
{
    private final SqlSourceConnection connection;
    private ResultSet resultSet;
    private int updateCount = -1;
    private int maxRows;
    private int queryTimeout;
    private boolean closed;

    public SqlSourceStatement(SqlSourceConnection connection)
    {
        this.connection = connection;
    }

    private void open()
            throws SQLException
    {
        if (closed) {
            throw new SQLException("Statement is closed");
        }
        connection.ensureOpen();
    }

    @Override
    public boolean execute(String sql)
            throws SQLException
    {
        open();
        resultSet = null;
        updateCount = -1;
        Rows rows = connection.getEngine().execute(sql);
        if (rows.isQuery()) {
            Rows limited = maxRows > 0 && rows.getValues().size() > maxRows
                    ? Rows.of(rows.getLabels(), rows.getTypes(), rows.getValues().subList(0, maxRows)) : rows;
            resultSet = new RowsResultSet(this, limited);
            return true;
        }
        updateCount = rows.getUpdateCount();
        return false;
    }

    @Override
    public ResultSet executeQuery(String sql)
            throws SQLException
    {
        if (!execute(sql)) {
            throw new SQLException("The statement returned no rows: " + sql);
        }
        return resultSet;
    }

    @Override
    public int executeUpdate(String sql)
            throws SQLException
    {
        if (execute(sql)) {
            throw new SQLException("The statement returned rows: " + sql);
        }
        return updateCount;
    }

    @Override
    public ResultSet getResultSet()
    {
        return resultSet;
    }

    @Override
    public int getUpdateCount()
    {
        return updateCount;
    }

    @Override
    public boolean getMoreResults()
    {
        resultSet = null;
        updateCount = -1;
        return false;
    }

    @Override
    public Connection getConnection()
    {
        return connection;
    }

    @Override
    public void close()
    {
        closed = true;
        resultSet = null;
    }

    @Override
    public boolean isClosed()
    {
        return closed;
    }

    @Override
    public int getMaxRows()
    {
        return maxRows;
    }

    @Override
    public void setMaxRows(int max)
    {
        this.maxRows = Math.max(0, max);
    }

    @Override
    public int getQueryTimeout()
    {
        return queryTimeout;
    }

    @Override
    public void setQueryTimeout(int seconds)
    {
        this.queryTimeout = seconds;
    }

    @Override
    public int getFetchSize()
    {
        return 0;
    }

    @Override
    public void setFetchSize(int rows)
    {
    }

    @Override
    public void setEscapeProcessing(boolean enable)
    {
    }

    @Override
    public void cancel()
    {
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
    public int getResultSetType()
    {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public int getResultSetConcurrency()
    {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
    {
        return iface.isInstance(this);
    }

    @Override
    public <T> T unwrap(Class<T> iface)
            throws SQLException
    {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }
}
