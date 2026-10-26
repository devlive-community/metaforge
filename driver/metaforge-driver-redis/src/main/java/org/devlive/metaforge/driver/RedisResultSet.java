package org.devlive.metaforge.driver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings(value = {"DM_DEFAULT_ENCODING", "EI_EXPOSE_REP2"})
public class RedisResultSet
        implements ResultSet
{
    private final List<Map<String, Object>> rows;
    private final List<String> columnNames;
    private Map<String, Object> current;
    private int position = -1;
    private boolean isClosed = false;
    private ResultSetMetaData metadata;

    public RedisResultSet(List<Map<String, Object>> rows)
    {
        this.rows = rows;
        this.columnNames = new ArrayList<>();
        this.current = null;
        this.metadata = null;

        if (!rows.isEmpty()) {
            Map<String, Object> first = rows.get(0);
            columnNames.addAll(first.keySet());
            this.metadata = new RedisResultSetMetaData(columnNames, first);
        }
    }

    @Override
    public boolean next()
            throws SQLException
    {
        checkClosed();

        position++;
        if (position < rows.size()) {
            current = rows.get(position);
            return true;
        }
        current = null;
        return false;
    }

    @Override
    public String getString(String columnLabel)
            throws SQLException
    {
        checkClosed();

        if (current == null) {
            throw new SQLException("No current row");
        }
        Object value = current.get(columnLabel);
        return value == null ? null : value.toString();
    }

    @Override
    public boolean getBoolean(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    @Override
    public byte getByte(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).byteValue();
        }
        return Byte.parseByte(value.toString());
    }

    @Override
    public short getShort(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }
        return Short.parseShort(value.toString());
    }

    @Override
    public int getInt(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    @Override
    public long getLong(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    @Override
    public float getFloat(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return Float.parseFloat(value.toString());
    }

    @Override
    public double getDouble(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    @Override
    public byte[] getBytes(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        return value.toString().getBytes();
    }

    @Override
    public Date getDate(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        return Date.valueOf(value.toString());
    }

    @Override
    public Time getTime(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return null;
        }
        if (value instanceof Time) {
            return (Time) value;
        }
        return Time.valueOf(value.toString());
    }

    @Override
    public Timestamp getTimestamp(String columnLabel)
            throws SQLException
    {
        checkClosed();

        Object value = current.get(columnLabel);
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        return Timestamp.valueOf(value.toString());
    }

    @Override
    public InputStream getAsciiStream(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public InputStream getBinaryStream(String columnLabel)
            throws SQLException
    {
        return null;
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
    public String getCursorName()
            throws SQLException
    {
        return "";
    }

    @Override
    public ResultSetMetaData getMetaData()
            throws SQLException
    {
        checkClosed();

        return metadata;
    }

    @Override
    public Object getObject(int columnIndex)
            throws SQLException
    {
        checkClosed();

        String columnName = getColumnName(columnIndex);
        return current.get(columnName);
    }

    @Override
    public Object getObject(String columnLabel)
            throws SQLException
    {
        checkClosed();

        return current.get(columnLabel);
    }

    @Override
    public int findColumn(String columnLabel)
            throws SQLException
    {
        int index = columnNames.indexOf(columnLabel);
        if (index == -1) {
            throw new SQLException("Column not found: " + columnLabel);
        }
        return index + 1;
    }

    @Override
    public Reader getCharacterStream(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public Reader getCharacterStream(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex)
            throws SQLException
    {
        return getBigDecimal(getColumnName(columnIndex), 0);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel)
            throws SQLException
    {
        return getBigDecimal(columnLabel, 0);
    }

    @Override
    public boolean isBeforeFirst()
            throws SQLException
    {
        return position < 0;
    }

    @Override
    public boolean isAfterLast()
            throws SQLException
    {
        return position >= rows.size();
    }

    @Override
    public boolean isFirst()
            throws SQLException
    {
        return position == 0;
    }

    @Override
    public boolean isLast()
            throws SQLException
    {
        return position == rows.size() - 1;
    }

    @Override
    public void beforeFirst()
            throws SQLException
    {
        position = -1;
        current = null;
    }

    @Override
    public void afterLast()
            throws SQLException
    {
        position = rows.size();
        current = null;
    }

    @Override
    public boolean first()
            throws SQLException
    {
        if (rows.isEmpty()) {
            return false;
        }
        position = 0;
        current = rows.get(0);
        return true;
    }

    @Override
    public boolean last()
            throws SQLException
    {
        if (rows.isEmpty()) {
            return false;
        }
        position = rows.size() - 1;
        current = rows.get(position);
        return true;
    }

    @Override
    public int getRow()
            throws SQLException
    {
        return position + 1;
    }

    @Override
    public boolean absolute(int row)
            throws SQLException
    {
        if (row > 0 && row <= rows.size()) {
            position = row - 1;
            current = rows.get(position);
            return true;
        }
        return false;
    }

    @Override
    public boolean relative(int rows)
            throws SQLException
    {
        return absolute(position + 1 + rows);
    }

    @Override
    public boolean previous()
            throws SQLException
    {
        if (position > 0) {
            position--;
            current = rows.get(position);
            return true;
        }
        return false;
    }

    @Override
    public void setFetchDirection(int direction)
            throws SQLException
    {}

    @Override
    public int getFetchDirection()
            throws SQLException
    {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(int rows)
            throws SQLException
    {}

    @Override
    public int getFetchSize()
            throws SQLException
    {
        return 0;
    }

    @Override
    public int getType()
            throws SQLException
    {
        return ResultSet.TYPE_SCROLL_INSENSITIVE;
    }

    @Override
    public int getConcurrency()
            throws SQLException
    {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean rowUpdated()
            throws SQLException
    {
        return false;
    }

    @Override
    public boolean rowInserted()
            throws SQLException
    {
        return false;
    }

    @Override
    public boolean rowDeleted()
            throws SQLException
    {
        return false;
    }

    @Override
    public void updateNull(int columnIndex)
            throws SQLException
    {}

    @Override
    public void updateBoolean(int columnIndex, boolean x)
            throws SQLException
    {}

    @Override
    public void updateByte(int columnIndex, byte x)
            throws SQLException
    {}

    @Override
    public void updateShort(int columnIndex, short x)
            throws SQLException
    {}

    @Override
    public void updateInt(int columnIndex, int x)
            throws SQLException
    {}

    @Override
    public void updateLong(int columnIndex, long x)
            throws SQLException
    {}

    @Override
    public void updateFloat(int columnIndex, float x)
            throws SQLException
    {}

    @Override
    public void updateDouble(int columnIndex, double x)
            throws SQLException
    {}

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x)
            throws SQLException
    {}

    @Override
    public void updateString(int columnIndex, String x)
            throws SQLException
    {}

    @Override
    public void updateBytes(int columnIndex, byte[] x)
            throws SQLException
    {}

    @Override
    public void updateDate(int columnIndex, Date x)
            throws SQLException
    {}

    @Override
    public void updateTime(int columnIndex, Time x)
            throws SQLException
    {}

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x)
            throws SQLException
    {}

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length)
            throws SQLException
    {}

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length)
            throws SQLException
    {}

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length)
            throws SQLException
    {}

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength)
            throws SQLException
    {}

    @Override
    public void updateObject(int columnIndex, Object x)
            throws SQLException
    {}

    @Override
    public void updateNull(String columnLabel)
            throws SQLException
    {}

    @Override
    public void updateBoolean(String columnLabel, boolean x)
            throws SQLException
    {}

    @Override
    public void updateByte(String columnLabel, byte x)
            throws SQLException
    {}

    @Override
    public void updateShort(String columnLabel, short x)
            throws SQLException
    {}

    @Override
    public void updateInt(String columnLabel, int x)
            throws SQLException
    {}

    @Override
    public void updateLong(String columnLabel, long x)
            throws SQLException
    {}

    @Override
    public void updateFloat(String columnLabel, float x)
            throws SQLException
    {}

    @Override
    public void updateDouble(String columnLabel, double x)
            throws SQLException
    {}

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x)
            throws SQLException
    {}

    @Override
    public void updateString(String columnLabel, String x)
            throws SQLException
    {}

    @Override
    public void updateBytes(String columnLabel, byte[] x)
            throws SQLException
    {}

    @Override
    public void updateDate(String columnLabel, Date x)
            throws SQLException
    {}

    @Override
    public void updateTime(String columnLabel, Time x)
            throws SQLException
    {}

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x)
            throws SQLException
    {}

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length)
            throws SQLException
    {}

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length)
            throws SQLException
    {}

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length)
            throws SQLException
    {}

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength)
            throws SQLException
    {}

    @Override
    public void updateObject(String columnLabel, Object x)
            throws SQLException
    {}

    @Override
    public void insertRow()
            throws SQLException
    {}

    @Override
    public void updateRow()
            throws SQLException
    {}

    @Override
    public void deleteRow()
            throws SQLException
    {}

    @Override
    public void refreshRow()
            throws SQLException
    {}

    @Override
    public void cancelRowUpdates()
            throws SQLException
    {}

    @Override
    public void moveToInsertRow()
            throws SQLException
    {}

    @Override
    public void moveToCurrentRow()
            throws SQLException
    {}

    @Override
    public Statement getStatement()
            throws SQLException
    {
        return null;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map)
            throws SQLException
    {
        return getObject(columnIndex);
    }

    @Override
    public Ref getRef(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public Blob getBlob(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public Clob getClob(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public Array getArray(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map)
            throws SQLException
    {
        return getObject(columnLabel);
    }

    @Override
    public Ref getRef(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public Blob getBlob(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public Clob getClob(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public Array getArray(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal)
            throws SQLException
    {
        return getDate(getColumnName(columnIndex));
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal)
            throws SQLException
    {
        return getDate(columnLabel);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal)
            throws SQLException
    {
        return getTime(getColumnName(columnIndex));
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal)
            throws SQLException
    {
        return getTime(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal)
            throws SQLException
    {
        return getTimestamp(getColumnName(columnIndex));
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal)
            throws SQLException
    {
        return getTimestamp(columnLabel);
    }

    @Override
    public URL getURL(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public URL getURL(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public void updateRef(int columnIndex, Ref x)
            throws SQLException
    {}

    @Override
    public void updateRef(String columnLabel, Ref x)
            throws SQLException
    {}

    @Override
    public void updateBlob(int columnIndex, Blob x)
            throws SQLException
    {}

    @Override
    public void updateBlob(String columnLabel, Blob x)
            throws SQLException
    {}

    @Override
    public void updateClob(int columnIndex, Clob x)
            throws SQLException
    {}

    @Override
    public void updateClob(String columnLabel, Clob x)
            throws SQLException
    {}

    @Override
    public void updateArray(int columnIndex, Array x)
            throws SQLException
    {}

    @Override
    public void updateArray(String columnLabel, Array x)
            throws SQLException
    {}

    @Override
    public RowId getRowId(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public RowId getRowId(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public void updateRowId(int columnIndex, RowId x)
            throws SQLException
    {}

    @Override
    public void updateRowId(String columnLabel, RowId x)
            throws SQLException
    {}

    @Override
    public int getHoldability()
            throws SQLException
    {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public boolean isClosed()
            throws SQLException
    {
        return isClosed;
    }

    @Override
    public void updateNString(int columnIndex, String nString)
            throws SQLException
    {}

    @Override
    public void updateNString(String columnLabel, String nString)
            throws SQLException
    {}

    @Override
    public void updateNClob(int columnIndex, NClob nClob)
            throws SQLException
    {}

    @Override
    public void updateNClob(String columnLabel, NClob nClob)
            throws SQLException
    {}

    @Override
    public NClob getNClob(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public NClob getNClob(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public SQLXML getSQLXML(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public SQLXML getSQLXML(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject)
            throws SQLException
    {}

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject)
            throws SQLException
    {}

    @Override
    public String getNString(int columnIndex)
            throws SQLException
    {
        return getString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel)
            throws SQLException
    {
        return getString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public Reader getNCharacterStream(String columnLabel)
            throws SQLException
    {
        return null;
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length)
            throws SQLException
    {}

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length)
            throws SQLException
    {}

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length)
            throws SQLException
    {}

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length)
            throws SQLException
    {}

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length)
            throws SQLException
    {}

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length)
            throws SQLException
    {}

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length)
            throws SQLException
    {}

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length)
            throws SQLException
    {}

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length)
            throws SQLException
    {}

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length)
            throws SQLException
    {}

    @Override
    public void updateClob(int columnIndex, Reader reader, long length)
            throws SQLException
    {}

    @Override
    public void updateClob(String columnLabel, Reader reader, long length)
            throws SQLException
    {}

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length)
            throws SQLException
    {}

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length)
            throws SQLException
    {}

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x)
            throws SQLException
    {}

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader)
            throws SQLException
    {}

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x)
            throws SQLException
    {}

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x)
            throws SQLException
    {}

    @Override
    public void updateCharacterStream(int columnIndex, Reader x)
            throws SQLException
    {}

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x)
            throws SQLException
    {}

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x)
            throws SQLException
    {}

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader)
            throws SQLException
    {}

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream)
            throws SQLException
    {}

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream)
            throws SQLException
    {}

    @Override
    public void updateClob(int columnIndex, Reader reader)
            throws SQLException
    {}

    @Override
    public void updateClob(String columnLabel, Reader reader)
            throws SQLException
    {}

    @Override
    public void updateNClob(int columnIndex, Reader reader)
            throws SQLException
    {}

    @Override
    public void updateNClob(String columnLabel, Reader reader)
            throws SQLException
    {}

    @Override
    public <T> T getObject(int columnIndex, Class<T> type)
            throws SQLException
    {
        return type.cast(getObject(columnIndex));
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type)
            throws SQLException
    {
        return type.cast(getObject(columnLabel));
    }

    private void checkClosed()
            throws SQLException
    {
        if (isClosed) {
            throw new SQLException("ResultSet is closed");
        }
    }

    @Override
    public void close()
            throws SQLException
    {
        isClosed = true;
    }

    @Override
    public boolean wasNull()
            throws SQLException
    {
        return false;
    }

    @Override
    public String getString(int columnIndex)
            throws SQLException
    {
        return getString(getColumnName(columnIndex));
    }

    @Override
    public boolean getBoolean(int columnIndex)
            throws SQLException
    {
        return getBoolean(getColumnName(columnIndex));
    }

    @Override
    public byte getByte(int columnIndex)
            throws SQLException
    {
        return getByte(getColumnName(columnIndex));
    }

    @Override
    public short getShort(int columnIndex)
            throws SQLException
    {
        return getShort(getColumnName(columnIndex));
    }

    @Override
    public int getInt(int columnIndex)
            throws SQLException
    {
        return getInt(getColumnName(columnIndex));
    }

    @Override
    public long getLong(int columnIndex)
            throws SQLException
    {
        return getLong(getColumnName(columnIndex));
    }

    @Override
    public float getFloat(int columnIndex)
            throws SQLException
    {
        return getFloat(getColumnName(columnIndex));
    }

    @Override
    public double getDouble(int columnIndex)
            throws SQLException
    {
        return getDouble(getColumnName(columnIndex));
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale)
            throws SQLException
    {
        return getBigDecimal(getColumnName(columnIndex), scale);
    }

    @Override
    public byte[] getBytes(int columnIndex)
            throws SQLException
    {
        return getBytes(getColumnName(columnIndex));
    }

    @Override
    public Date getDate(int columnIndex)
            throws SQLException
    {
        return getDate(getColumnName(columnIndex));
    }

    @Override
    public Time getTime(int columnIndex)
            throws SQLException
    {
        return getTime(getColumnName(columnIndex));
    }

    @Override
    public Timestamp getTimestamp(int columnIndex)
            throws SQLException
    {
        return getTimestamp(getColumnName(columnIndex));
    }

    @Override
    public InputStream getAsciiStream(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex)
            throws SQLException
    {
        return null;
    }

    @Override
    public InputStream getBinaryStream(int columnIndex)
            throws SQLException
    {
        return null;
    }

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

    private String getColumnName(int columnIndex)
    {
        if (columnIndex < 1 || columnIndex > columnNames.size()) {
            throw new IllegalArgumentException("Invalid column index: " + columnIndex);
        }
        return columnNames.get(columnIndex - 1);
    }
}
