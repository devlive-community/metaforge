package org.devlive.metaforge.driver.sql.base;

/**
 * Every {@link java.sql.Connection} method refuses until a driver overrides it:
 * with {@link java.sql.SQLFeatureNotSupportedException} where the interface allows it. Generated
 * from the JDK interface; do not edit by hand.
 */
public abstract class BaseConnection
        implements java.sql.Connection
{
    @Override
    public void abort(java.util.concurrent.Executor arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.abort");
    }

    @Override
    public void clearWarnings()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.clearWarnings");
    }

    @Override
    public void close()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.close");
    }

    @Override
    public void commit()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.commit");
    }

    @Override
    public java.sql.Array createArrayOf(String arg0, Object[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.createArrayOf");
    }

    @Override
    public java.sql.Blob createBlob()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.createBlob");
    }

    @Override
    public java.sql.Clob createClob()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.createClob");
    }

    @Override
    public java.sql.NClob createNClob()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.createNClob");
    }

    @Override
    public java.sql.SQLXML createSQLXML()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.createSQLXML");
    }

    @Override
    public java.sql.Statement createStatement()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.createStatement");
    }

    @Override
    public java.sql.Statement createStatement(int arg0, int arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.createStatement");
    }

    @Override
    public java.sql.Statement createStatement(int arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.createStatement");
    }

    @Override
    public java.sql.Struct createStruct(String arg0, Object[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.createStruct");
    }

    @Override
    public boolean getAutoCommit()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getAutoCommit");
    }

    @Override
    public String getCatalog()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getCatalog");
    }

    @Override
    public java.util.Properties getClientInfo()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getClientInfo");
    }

    @Override
    public String getClientInfo(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getClientInfo");
    }

    @Override
    public int getHoldability()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getHoldability");
    }

    @Override
    public java.sql.DatabaseMetaData getMetaData()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getMetaData");
    }

    @Override
    public int getNetworkTimeout()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getNetworkTimeout");
    }

    @Override
    public String getSchema()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getSchema");
    }

    @Override
    public int getTransactionIsolation()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getTransactionIsolation");
    }

    @Override
    public java.util.Map<String, Class<?>> getTypeMap()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getTypeMap");
    }

    @Override
    public java.sql.SQLWarning getWarnings()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.getWarnings");
    }

    @Override
    public boolean isClosed()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.isClosed");
    }

    @Override
    public boolean isReadOnly()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.isReadOnly");
    }

    @Override
    public boolean isValid(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.isValid");
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.isWrapperFor");
    }

    @Override
    public String nativeSQL(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.nativeSQL");
    }

    @Override
    public java.sql.CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.prepareCall");
    }

    @Override
    public java.sql.CallableStatement prepareCall(String arg0, int arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.prepareCall");
    }

    @Override
    public java.sql.CallableStatement prepareCall(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.prepareCall");
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String arg0, int[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.prepareStatement");
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String arg0, String[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.prepareStatement");
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.prepareStatement");
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String arg0, int arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.prepareStatement");
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.prepareStatement");
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.prepareStatement");
    }

    @Override
    public void releaseSavepoint(java.sql.Savepoint arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.releaseSavepoint");
    }

    @Override
    public void rollback()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.rollback");
    }

    @Override
    public void rollback(java.sql.Savepoint arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.rollback");
    }

    @Override
    public void setAutoCommit(boolean arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setAutoCommit");
    }

    @Override
    public void setCatalog(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setCatalog");
    }

    @Override
    public void setClientInfo(String arg0, String arg1)
            throws java.sql.SQLClientInfoException
    {
        throw new java.sql.SQLClientInfoException("Connection.setClientInfo is not supported", null);
    }

    @Override
    public void setClientInfo(java.util.Properties arg0)
            throws java.sql.SQLClientInfoException
    {
        throw new java.sql.SQLClientInfoException("Connection.setClientInfo is not supported", null);
    }

    @Override
    public void setHoldability(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setHoldability");
    }

    @Override
    public void setNetworkTimeout(java.util.concurrent.Executor arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setNetworkTimeout");
    }

    @Override
    public void setReadOnly(boolean arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setReadOnly");
    }

    @Override
    public java.sql.Savepoint setSavepoint()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setSavepoint");
    }

    @Override
    public java.sql.Savepoint setSavepoint(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setSavepoint");
    }

    @Override
    public void setSchema(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setSchema");
    }

    @Override
    public void setTransactionIsolation(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setTransactionIsolation");
    }

    @Override
    public void setTypeMap(java.util.Map<String, Class<?>> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.setTypeMap");
    }

    @Override
    public <T> T unwrap(Class<T> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("Connection.unwrap");
    }
}
