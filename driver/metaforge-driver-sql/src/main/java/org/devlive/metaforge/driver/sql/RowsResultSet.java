package org.devlive.metaforge.driver.sql;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.driver.sql.base.BaseResultSet;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** A forward-only, read-only result set over rows held in memory. */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "JDBC objects share their connection, statement and source")
public class RowsResultSet
        extends BaseResultSet
{
    private final List<String> labels;
    private final List<List<Object>> rows;
    private final RowsResultSetMetaData metaData;
    private final Statement statement;
    private int cursor = -1;
    private boolean closed;
    private boolean wasNull;

    public RowsResultSet(Statement statement, Rows rows)
    {
        this.statement = statement;
        this.labels = rows.getLabels();
        this.rows = new ArrayList<>(rows.getValues());
        this.metaData = new RowsResultSetMetaData(rows.getLabels(), rows.getTypes());
    }

    public static RowsResultSet of(List<String> labels, List<String> types, List<List<Object>> values)
    {
        return new RowsResultSet(null, Rows.of(labels, types, values));
    }

    private Object value(int column)
            throws SQLException
    {
        if (closed) {
            throw new SQLException("Result set is closed");
        }
        if (cursor < 0 || cursor >= rows.size()) {
            throw new SQLException("No current row");
        }
        if (column < 1 || column > labels.size()) {
            throw new SQLException("Column index " + column + " is out of range");
        }
        List<Object> row = rows.get(cursor);
        Object value = column <= row.size() ? row.get(column - 1) : null;
        wasNull = value == null;
        return value;
    }

    private Number number(int column)
            throws SQLException
    {
        Object value = value(column);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        }
        try {
            return new BigDecimal(value.toString().trim());
        }
        catch (NumberFormatException error) {
            throw new SQLException("Value " + value + " is not a number", error);
        }
    }

    @Override
    public boolean next()
            throws SQLException
    {
        if (closed) {
            throw new SQLException("Result set is closed");
        }
        if (cursor < rows.size()) {
            cursor++;
        }
        return cursor < rows.size();
    }

    @Override
    public void close()
    {
        closed = true;
    }

    @Override
    public boolean isClosed()
    {
        return closed;
    }

    @Override
    public boolean wasNull()
    {
        return wasNull;
    }

    @Override
    public int findColumn(String label)
            throws SQLException
    {
        for (int i = 0; i < labels.size(); i++) {
            if (labels.get(i).equalsIgnoreCase(label)) {
                return i + 1;
            }
        }
        throw new SQLException("Unknown column " + label);
    }

    @Override
    public ResultSetMetaData getMetaData()
    {
        return metaData;
    }

    @Override
    public Statement getStatement()
    {
        return statement;
    }

    @Override
    public Object getObject(int column)
            throws SQLException
    {
        return value(column);
    }

    @Override
    public Object getObject(String label)
            throws SQLException
    {
        return getObject(findColumn(label));
    }

    @Override
    public String getString(int column)
            throws SQLException
    {
        Object value = value(column);
        if (value instanceof byte[]) {
            return new String((byte[]) value, StandardCharsets.UTF_8);
        }
        return value == null ? null : value.toString();
    }

    @Override
    public String getString(String label)
            throws SQLException
    {
        return getString(findColumn(label));
    }

    @Override
    public int getInt(int column)
            throws SQLException
    {
        Number value = number(column);
        return value == null ? 0 : value.intValue();
    }

    @Override
    public int getInt(String label)
            throws SQLException
    {
        return getInt(findColumn(label));
    }

    @Override
    public long getLong(int column)
            throws SQLException
    {
        Number value = number(column);
        return value == null ? 0 : value.longValue();
    }

    @Override
    public long getLong(String label)
            throws SQLException
    {
        return getLong(findColumn(label));
    }

    @Override
    public short getShort(int column)
            throws SQLException
    {
        Number value = number(column);
        return value == null ? 0 : value.shortValue();
    }

    @Override
    public short getShort(String label)
            throws SQLException
    {
        return getShort(findColumn(label));
    }

    @Override
    public byte getByte(int column)
            throws SQLException
    {
        Number value = number(column);
        return value == null ? 0 : value.byteValue();
    }

    @Override
    public byte getByte(String label)
            throws SQLException
    {
        return getByte(findColumn(label));
    }

    @Override
    public double getDouble(int column)
            throws SQLException
    {
        Number value = number(column);
        return value == null ? 0 : value.doubleValue();
    }

    @Override
    public double getDouble(String label)
            throws SQLException
    {
        return getDouble(findColumn(label));
    }

    @Override
    public float getFloat(int column)
            throws SQLException
    {
        Number value = number(column);
        return value == null ? 0 : value.floatValue();
    }

    @Override
    public float getFloat(String label)
            throws SQLException
    {
        return getFloat(findColumn(label));
    }

    @Override
    public BigDecimal getBigDecimal(int column)
            throws SQLException
    {
        Number value = number(column);
        return value == null ? null : new BigDecimal(value.toString());
    }

    @Override
    public BigDecimal getBigDecimal(String label)
            throws SQLException
    {
        return getBigDecimal(findColumn(label));
    }

    @Override
    public boolean getBoolean(int column)
            throws SQLException
    {
        Object value = value(column);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return value != null && (value.toString().equalsIgnoreCase("true") || value.toString().equals("1"));
    }

    @Override
    public boolean getBoolean(String label)
            throws SQLException
    {
        return getBoolean(findColumn(label));
    }

    @Override
    public byte[] getBytes(int column)
            throws SQLException
    {
        Object value = value(column);
        if (value instanceof byte[]) {
            return ((byte[]) value).clone();
        }
        return value == null ? null : value.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getBytes(String label)
            throws SQLException
    {
        return getBytes(findColumn(label));
    }

    @Override
    public Timestamp getTimestamp(int column)
            throws SQLException
    {
        Object value = value(column);
        if (value == null || value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime());
        }
        if (value instanceof Number) {
            return new Timestamp(((Number) value).longValue());
        }
        try {
            return Timestamp.valueOf(value.toString());
        }
        catch (IllegalArgumentException error) {
            throw new SQLException("Value " + value + " is not a timestamp", error);
        }
    }

    @Override
    public Timestamp getTimestamp(String label)
            throws SQLException
    {
        return getTimestamp(findColumn(label));
    }

    @Override
    public Date getDate(int column)
            throws SQLException
    {
        Timestamp value = getTimestamp(column);
        return value == null ? null : new Date(value.getTime());
    }

    @Override
    public Date getDate(String label)
            throws SQLException
    {
        return getDate(findColumn(label));
    }

    @Override
    public Time getTime(int column)
            throws SQLException
    {
        Timestamp value = getTimestamp(column);
        return value == null ? null : new Time(value.getTime());
    }

    @Override
    public Time getTime(String label)
            throws SQLException
    {
        return getTime(findColumn(label));
    }

    @Override
    public int getType()
    {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public int getConcurrency()
    {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getHoldability()
    {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public int getFetchDirection()
    {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchDirection(int direction)
    {
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
    public int getRow()
    {
        return cursor >= 0 && cursor < rows.size() ? cursor + 1 : 0;
    }

    @Override
    public boolean isBeforeFirst()
    {
        return cursor < 0 && !rows.isEmpty();
    }

    @Override
    public boolean isAfterLast()
    {
        return cursor >= rows.size() && !rows.isEmpty();
    }

    @Override
    public boolean isFirst()
    {
        return cursor == 0 && !rows.isEmpty();
    }

    @Override
    public boolean isLast()
    {
        return cursor == rows.size() - 1 && !rows.isEmpty();
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
