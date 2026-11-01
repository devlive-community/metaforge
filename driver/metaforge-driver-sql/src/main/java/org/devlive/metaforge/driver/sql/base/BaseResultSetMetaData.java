package org.devlive.metaforge.driver.sql.base;

/**
 * Every {@link java.sql.ResultSetMetaData} method refuses until a driver overrides it:
 * with {@link java.sql.SQLFeatureNotSupportedException} where the interface allows it. Generated
 * from the JDK interface; do not edit by hand.
 */
public abstract class BaseResultSetMetaData
        implements java.sql.ResultSetMetaData
{
    @Override
    public String getCatalogName(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getCatalogName");
    }

    @Override
    public String getColumnClassName(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getColumnClassName");
    }

    @Override
    public int getColumnCount()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getColumnCount");
    }

    @Override
    public int getColumnDisplaySize(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getColumnDisplaySize");
    }

    @Override
    public String getColumnLabel(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getColumnLabel");
    }

    @Override
    public String getColumnName(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getColumnName");
    }

    @Override
    public int getColumnType(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getColumnType");
    }

    @Override
    public String getColumnTypeName(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getColumnTypeName");
    }

    @Override
    public int getPrecision(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getPrecision");
    }

    @Override
    public int getScale(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getScale");
    }

    @Override
    public String getSchemaName(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getSchemaName");
    }

    @Override
    public String getTableName(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.getTableName");
    }

    @Override
    public boolean isAutoIncrement(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isAutoIncrement");
    }

    @Override
    public boolean isCaseSensitive(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isCaseSensitive");
    }

    @Override
    public boolean isCurrency(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isCurrency");
    }

    @Override
    public boolean isDefinitelyWritable(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isDefinitelyWritable");
    }

    @Override
    public int isNullable(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isNullable");
    }

    @Override
    public boolean isReadOnly(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isReadOnly");
    }

    @Override
    public boolean isSearchable(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isSearchable");
    }

    @Override
    public boolean isSigned(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isSigned");
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isWrapperFor");
    }

    @Override
    public boolean isWritable(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.isWritable");
    }

    @Override
    public <T> T unwrap(Class<T> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSetMetaData.unwrap");
    }
}
