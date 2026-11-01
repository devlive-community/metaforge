package org.devlive.metaforge.driver.sql.base;

/**
 * Every {@link java.sql.ResultSet} method refuses until a driver overrides it:
 * with {@link java.sql.SQLFeatureNotSupportedException} where the interface allows it. Generated
 * from the JDK interface; do not edit by hand.
 */
public abstract class BaseResultSet
        implements java.sql.ResultSet
{
    @Override
    public boolean absolute(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.absolute");
    }

    @Override
    public void afterLast()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.afterLast");
    }

    @Override
    public void beforeFirst()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.beforeFirst");
    }

    @Override
    public void cancelRowUpdates()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.cancelRowUpdates");
    }

    @Override
    public void clearWarnings()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.clearWarnings");
    }

    @Override
    public void close()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.close");
    }

    @Override
    public void deleteRow()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.deleteRow");
    }

    @Override
    public int findColumn(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.findColumn");
    }

    @Override
    public boolean first()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.first");
    }

    @Override
    public java.sql.Array getArray(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getArray");
    }

    @Override
    public java.sql.Array getArray(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getArray");
    }

    @Override
    public java.io.InputStream getAsciiStream(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getAsciiStream");
    }

    @Override
    public java.io.InputStream getAsciiStream(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getAsciiStream");
    }

    @Override
    public java.math.BigDecimal getBigDecimal(String arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBigDecimal");
    }

    @Override
    public java.math.BigDecimal getBigDecimal(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBigDecimal");
    }

    @Override
    public java.math.BigDecimal getBigDecimal(int arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBigDecimal");
    }

    @Override
    public java.math.BigDecimal getBigDecimal(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBigDecimal");
    }

    @Override
    public java.io.InputStream getBinaryStream(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBinaryStream");
    }

    @Override
    public java.io.InputStream getBinaryStream(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBinaryStream");
    }

    @Override
    public java.sql.Blob getBlob(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBlob");
    }

    @Override
    public java.sql.Blob getBlob(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBlob");
    }

    @Override
    public boolean getBoolean(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBoolean");
    }

    @Override
    public boolean getBoolean(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBoolean");
    }

    @Override
    public byte getByte(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getByte");
    }

    @Override
    public byte getByte(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getByte");
    }

    @Override
    public byte[] getBytes(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBytes");
    }

    @Override
    public byte[] getBytes(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getBytes");
    }

    @Override
    public java.io.Reader getCharacterStream(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getCharacterStream");
    }

    @Override
    public java.io.Reader getCharacterStream(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getCharacterStream");
    }

    @Override
    public java.sql.Clob getClob(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getClob");
    }

    @Override
    public java.sql.Clob getClob(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getClob");
    }

    @Override
    public int getConcurrency()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getConcurrency");
    }

    @Override
    public String getCursorName()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getCursorName");
    }

    @Override
    public java.sql.Date getDate(String arg0, java.util.Calendar arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getDate");
    }

    @Override
    public java.sql.Date getDate(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getDate");
    }

    @Override
    public java.sql.Date getDate(int arg0, java.util.Calendar arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getDate");
    }

    @Override
    public java.sql.Date getDate(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getDate");
    }

    @Override
    public double getDouble(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getDouble");
    }

    @Override
    public double getDouble(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getDouble");
    }

    @Override
    public int getFetchDirection()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getFetchDirection");
    }

    @Override
    public int getFetchSize()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getFetchSize");
    }

    @Override
    public float getFloat(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getFloat");
    }

    @Override
    public float getFloat(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getFloat");
    }

    @Override
    public int getHoldability()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getHoldability");
    }

    @Override
    public int getInt(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getInt");
    }

    @Override
    public int getInt(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getInt");
    }

    @Override
    public long getLong(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getLong");
    }

    @Override
    public long getLong(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getLong");
    }

    @Override
    public java.sql.ResultSetMetaData getMetaData()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getMetaData");
    }

    @Override
    public java.io.Reader getNCharacterStream(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getNCharacterStream");
    }

    @Override
    public java.io.Reader getNCharacterStream(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getNCharacterStream");
    }

    @Override
    public java.sql.NClob getNClob(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getNClob");
    }

    @Override
    public java.sql.NClob getNClob(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getNClob");
    }

    @Override
    public String getNString(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getNString");
    }

    @Override
    public String getNString(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getNString");
    }

    @Override
    public <T> T getObject(String arg0, Class<T> arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getObject");
    }

    @Override
    public Object getObject(String arg0, java.util.Map<String, Class<?>> arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getObject");
    }

    @Override
    public Object getObject(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getObject");
    }

    @Override
    public <T> T getObject(int arg0, Class<T> arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getObject");
    }

    @Override
    public Object getObject(int arg0, java.util.Map<String, Class<?>> arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getObject");
    }

    @Override
    public Object getObject(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getObject");
    }

    @Override
    public java.sql.Ref getRef(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getRef");
    }

    @Override
    public java.sql.Ref getRef(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getRef");
    }

    @Override
    public int getRow()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getRow");
    }

    @Override
    public java.sql.RowId getRowId(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getRowId");
    }

    @Override
    public java.sql.RowId getRowId(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getRowId");
    }

    @Override
    public java.sql.SQLXML getSQLXML(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getSQLXML");
    }

    @Override
    public java.sql.SQLXML getSQLXML(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getSQLXML");
    }

    @Override
    public short getShort(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getShort");
    }

    @Override
    public short getShort(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getShort");
    }

    @Override
    public java.sql.Statement getStatement()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getStatement");
    }

    @Override
    public String getString(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getString");
    }

    @Override
    public String getString(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getString");
    }

    @Override
    public java.sql.Time getTime(String arg0, java.util.Calendar arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getTime");
    }

    @Override
    public java.sql.Time getTime(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getTime");
    }

    @Override
    public java.sql.Time getTime(int arg0, java.util.Calendar arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getTime");
    }

    @Override
    public java.sql.Time getTime(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getTime");
    }

    @Override
    public java.sql.Timestamp getTimestamp(String arg0, java.util.Calendar arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getTimestamp");
    }

    @Override
    public java.sql.Timestamp getTimestamp(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getTimestamp");
    }

    @Override
    public java.sql.Timestamp getTimestamp(int arg0, java.util.Calendar arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getTimestamp");
    }

    @Override
    public java.sql.Timestamp getTimestamp(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getTimestamp");
    }

    @Override
    public int getType()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getType");
    }

    @Override
    public java.net.URL getURL(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getURL");
    }

    @Override
    public java.net.URL getURL(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getURL");
    }

    @Override
    public java.io.InputStream getUnicodeStream(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getUnicodeStream");
    }

    @Override
    public java.io.InputStream getUnicodeStream(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getUnicodeStream");
    }

    @Override
    public java.sql.SQLWarning getWarnings()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.getWarnings");
    }

    @Override
    public void insertRow()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.insertRow");
    }

    @Override
    public boolean isAfterLast()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.isAfterLast");
    }

    @Override
    public boolean isBeforeFirst()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.isBeforeFirst");
    }

    @Override
    public boolean isClosed()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.isClosed");
    }

    @Override
    public boolean isFirst()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.isFirst");
    }

    @Override
    public boolean isLast()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.isLast");
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.isWrapperFor");
    }

    @Override
    public boolean last()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.last");
    }

    @Override
    public void moveToCurrentRow()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.moveToCurrentRow");
    }

    @Override
    public void moveToInsertRow()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.moveToInsertRow");
    }

    @Override
    public boolean next()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.next");
    }

    @Override
    public boolean previous()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.previous");
    }

    @Override
    public void refreshRow()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.refreshRow");
    }

    @Override
    public boolean relative(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.relative");
    }

    @Override
    public boolean rowDeleted()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.rowDeleted");
    }

    @Override
    public boolean rowInserted()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.rowInserted");
    }

    @Override
    public boolean rowUpdated()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.rowUpdated");
    }

    @Override
    public void setFetchDirection(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.setFetchDirection");
    }

    @Override
    public void setFetchSize(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.setFetchSize");
    }

    @Override
    public <T> T unwrap(Class<T> arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.unwrap");
    }

    @Override
    public void updateArray(String arg0, java.sql.Array arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateArray");
    }

    @Override
    public void updateArray(int arg0, java.sql.Array arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateArray");
    }

    @Override
    public void updateAsciiStream(String arg0, java.io.InputStream arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateAsciiStream");
    }

    @Override
    public void updateAsciiStream(String arg0, java.io.InputStream arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateAsciiStream");
    }

    @Override
    public void updateAsciiStream(String arg0, java.io.InputStream arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateAsciiStream");
    }

    @Override
    public void updateAsciiStream(int arg0, java.io.InputStream arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateAsciiStream");
    }

    @Override
    public void updateAsciiStream(int arg0, java.io.InputStream arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateAsciiStream");
    }

    @Override
    public void updateAsciiStream(int arg0, java.io.InputStream arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateAsciiStream");
    }

    @Override
    public void updateBigDecimal(String arg0, java.math.BigDecimal arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBigDecimal");
    }

    @Override
    public void updateBigDecimal(int arg0, java.math.BigDecimal arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBigDecimal");
    }

    @Override
    public void updateBinaryStream(String arg0, java.io.InputStream arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(String arg0, java.io.InputStream arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(String arg0, java.io.InputStream arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(int arg0, java.io.InputStream arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(int arg0, java.io.InputStream arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(int arg0, java.io.InputStream arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBinaryStream");
    }

    @Override
    public void updateBlob(String arg0, java.io.InputStream arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBlob");
    }

    @Override
    public void updateBlob(String arg0, java.io.InputStream arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBlob");
    }

    @Override
    public void updateBlob(String arg0, java.sql.Blob arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBlob");
    }

    @Override
    public void updateBlob(int arg0, java.io.InputStream arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBlob");
    }

    @Override
    public void updateBlob(int arg0, java.io.InputStream arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBlob");
    }

    @Override
    public void updateBlob(int arg0, java.sql.Blob arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBlob");
    }

    @Override
    public void updateBoolean(String arg0, boolean arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBoolean");
    }

    @Override
    public void updateBoolean(int arg0, boolean arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBoolean");
    }

    @Override
    public void updateByte(String arg0, byte arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateByte");
    }

    @Override
    public void updateByte(int arg0, byte arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateByte");
    }

    @Override
    public void updateBytes(String arg0, byte[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBytes");
    }

    @Override
    public void updateBytes(int arg0, byte[] arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateBytes");
    }

    @Override
    public void updateCharacterStream(String arg0, java.io.Reader arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(String arg0, java.io.Reader arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(String arg0, java.io.Reader arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(int arg0, java.io.Reader arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(int arg0, java.io.Reader arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(int arg0, java.io.Reader arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateCharacterStream");
    }

    @Override
    public void updateClob(String arg0, java.io.Reader arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateClob");
    }

    @Override
    public void updateClob(String arg0, java.io.Reader arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateClob");
    }

    @Override
    public void updateClob(String arg0, java.sql.Clob arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateClob");
    }

    @Override
    public void updateClob(int arg0, java.io.Reader arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateClob");
    }

    @Override
    public void updateClob(int arg0, java.io.Reader arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateClob");
    }

    @Override
    public void updateClob(int arg0, java.sql.Clob arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateClob");
    }

    @Override
    public void updateDate(String arg0, java.sql.Date arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateDate");
    }

    @Override
    public void updateDate(int arg0, java.sql.Date arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateDate");
    }

    @Override
    public void updateDouble(String arg0, double arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateDouble");
    }

    @Override
    public void updateDouble(int arg0, double arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateDouble");
    }

    @Override
    public void updateFloat(String arg0, float arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateFloat");
    }

    @Override
    public void updateFloat(int arg0, float arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateFloat");
    }

    @Override
    public void updateInt(String arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateInt");
    }

    @Override
    public void updateInt(int arg0, int arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateInt");
    }

    @Override
    public void updateLong(String arg0, long arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateLong");
    }

    @Override
    public void updateLong(int arg0, long arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateLong");
    }

    @Override
    public void updateNCharacterStream(String arg0, java.io.Reader arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNCharacterStream");
    }

    @Override
    public void updateNCharacterStream(String arg0, java.io.Reader arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNCharacterStream");
    }

    @Override
    public void updateNCharacterStream(int arg0, java.io.Reader arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNCharacterStream");
    }

    @Override
    public void updateNCharacterStream(int arg0, java.io.Reader arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNCharacterStream");
    }

    @Override
    public void updateNClob(String arg0, java.io.Reader arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNClob");
    }

    @Override
    public void updateNClob(String arg0, java.io.Reader arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNClob");
    }

    @Override
    public void updateNClob(String arg0, java.sql.NClob arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNClob");
    }

    @Override
    public void updateNClob(int arg0, java.io.Reader arg1, long arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNClob");
    }

    @Override
    public void updateNClob(int arg0, java.io.Reader arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNClob");
    }

    @Override
    public void updateNClob(int arg0, java.sql.NClob arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNClob");
    }

    @Override
    public void updateNString(String arg0, String arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNString");
    }

    @Override
    public void updateNString(int arg0, String arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNString");
    }

    @Override
    public void updateNull(String arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNull");
    }

    @Override
    public void updateNull(int arg0)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateNull");
    }

    @Override
    public void updateObject(String arg0, Object arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateObject");
    }

    @Override
    public void updateObject(String arg0, Object arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateObject");
    }

    @Override
    public void updateObject(int arg0, Object arg1, int arg2)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateObject");
    }

    @Override
    public void updateObject(int arg0, Object arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateObject");
    }

    @Override
    public void updateRef(String arg0, java.sql.Ref arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateRef");
    }

    @Override
    public void updateRef(int arg0, java.sql.Ref arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateRef");
    }

    @Override
    public void updateRow()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateRow");
    }

    @Override
    public void updateRowId(String arg0, java.sql.RowId arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateRowId");
    }

    @Override
    public void updateRowId(int arg0, java.sql.RowId arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateRowId");
    }

    @Override
    public void updateSQLXML(String arg0, java.sql.SQLXML arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateSQLXML");
    }

    @Override
    public void updateSQLXML(int arg0, java.sql.SQLXML arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateSQLXML");
    }

    @Override
    public void updateShort(String arg0, short arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateShort");
    }

    @Override
    public void updateShort(int arg0, short arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateShort");
    }

    @Override
    public void updateString(String arg0, String arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateString");
    }

    @Override
    public void updateString(int arg0, String arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateString");
    }

    @Override
    public void updateTime(String arg0, java.sql.Time arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateTime");
    }

    @Override
    public void updateTime(int arg0, java.sql.Time arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateTime");
    }

    @Override
    public void updateTimestamp(String arg0, java.sql.Timestamp arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateTimestamp");
    }

    @Override
    public void updateTimestamp(int arg0, java.sql.Timestamp arg1)
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.updateTimestamp");
    }

    @Override
    public boolean wasNull()
            throws java.sql.SQLException
    {
        throw new java.sql.SQLFeatureNotSupportedException("ResultSet.wasNull");
    }
}
