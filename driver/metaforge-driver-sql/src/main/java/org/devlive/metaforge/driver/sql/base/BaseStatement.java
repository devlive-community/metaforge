package org.devlive.metaforge.driver.sql.base;

/**
 * Every {@link java.sql.Statement} method refuses until a driver overrides it:
 * with {@link java.sql.SQLFeatureNotSupportedException} where the interface allows it. Generated
 * from the JDK interface; do not edit by hand.
 */
public abstract class BaseStatement
        implements java.sql.Statement
{
    @Override
    public void addBatch(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.addBatch");
    }

    @Override
    public void cancel()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.cancel");
    }

    @Override
    public void clearBatch()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.clearBatch");
    }

    @Override
    public void clearWarnings()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.clearWarnings");
    }

    @Override
    public void close()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.close");
    }

    @Override
    public void closeOnCompletion()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.closeOnCompletion");
    }

    @Override
    public boolean execute(String arg0, int[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.execute");
    }

    @Override
    public boolean execute(String arg0, String[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.execute");
    }

    @Override
    public boolean execute(String arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.execute");
    }

    @Override
    public boolean execute(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.execute");
    }

    @Override
    public int[] executeBatch()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.executeBatch");
    }

    @Override
    public java.sql.ResultSet executeQuery(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.executeQuery");
    }

    @Override
    public int executeUpdate(String arg0, int[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.executeUpdate");
    }

    @Override
    public int executeUpdate(String arg0, String[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.executeUpdate");
    }

    @Override
    public int executeUpdate(String arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.executeUpdate");
    }

    @Override
    public int executeUpdate(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.executeUpdate");
    }

    @Override
    public java.sql.Connection getConnection()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getConnection");
    }

    @Override
    public int getFetchDirection()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getFetchDirection");
    }

    @Override
    public int getFetchSize()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getFetchSize");
    }

    @Override
    public java.sql.ResultSet getGeneratedKeys()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getGeneratedKeys");
    }

    @Override
    public int getMaxFieldSize()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getMaxFieldSize");
    }

    @Override
    public int getMaxRows()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getMaxRows");
    }

    @Override
    public boolean getMoreResults()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getMoreResults");
    }

    @Override
    public boolean getMoreResults(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getMoreResults");
    }

    @Override
    public int getQueryTimeout()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getQueryTimeout");
    }

    @Override
    public java.sql.ResultSet getResultSet()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getResultSet");
    }

    @Override
    public int getResultSetConcurrency()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getResultSetConcurrency");
    }

    @Override
    public int getResultSetHoldability()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getResultSetHoldability");
    }

    @Override
    public int getResultSetType()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getResultSetType");
    }

    @Override
    public int getUpdateCount()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getUpdateCount");
    }

    @Override
    public java.sql.SQLWarning getWarnings()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.getWarnings");
    }

    @Override
    public boolean isCloseOnCompletion()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.isCloseOnCompletion");
    }

    @Override
    public boolean isClosed()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.isClosed");
    }

    @Override
    public boolean isPoolable()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.isPoolable");
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.isWrapperFor");
    }

    @Override
    public void setCursorName(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.setCursorName");
    }

    @Override
    public void setEscapeProcessing(boolean arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.setEscapeProcessing");
    }

    @Override
    public void setFetchDirection(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.setFetchDirection");
    }

    @Override
    public void setFetchSize(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.setFetchSize");
    }

    @Override
    public void setMaxFieldSize(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.setMaxFieldSize");
    }

    @Override
    public void setMaxRows(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.setMaxRows");
    }

    @Override
    public void setPoolable(boolean arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.setPoolable");
    }

    @Override
    public void setQueryTimeout(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.setQueryTimeout");
    }

    @Override
    public <T> T unwrap(Class<T> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Statement.unwrap");
    }
}
