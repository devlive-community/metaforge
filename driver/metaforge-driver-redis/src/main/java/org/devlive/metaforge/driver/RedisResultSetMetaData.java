package org.devlive.metaforge.driver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings(value = {"NP_NONNULL_RETURN_VIOLATION", "EI_EXPOSE_REP2", "REC_CATCH_EXCEPTION"})
public class RedisResultSetMetaData
        implements ResultSetMetaData
{
    private final List<String> columnNames;
    private final Map<String, Object> sampleRow;

    public RedisResultSetMetaData(List<String> columnNames, Map<String, Object> sampleRow)
    {
        this.columnNames = columnNames;
        this.sampleRow = sampleRow;
    }

    @Override
    public int getColumnCount()
            throws SQLException
    {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column)
            throws SQLException
    {
        checkColumnIndex(column);
        return columnNames.get(column - 1);
    }

    @Override
    public String getColumnLabel(int column)
            throws SQLException
    {
        return getColumnName(column);
    }

    @Override
    public int getColumnType(int column)
            throws SQLException
    {
        checkColumnIndex(column);

        String columnName = columnNames.get(column - 1);
        Object value = sampleRow.get(columnName);
        return RedisTypeHelper.getJdbcType(value);
    }

    @Override
    public String getColumnTypeName(int column)
            throws SQLException
    {
        checkColumnIndex(column);

        String columnName = columnNames.get(column - 1);
        Object value = sampleRow.get(columnName);
        return RedisTypeHelper.getTypeName(value);
    }

    @Override
    public String getColumnClassName(int column)
            throws SQLException
    {
        checkColumnIndex(column);

        String columnName = columnNames.get(column - 1);
        Object value = sampleRow.get(columnName);
        return RedisTypeHelper.getJavaClassName(value);
    }

    @Override
    public int isNullable(int column)
            throws SQLException
    {
        return columnNullable;
    }

    @Override
    public boolean isAutoIncrement(int column)
            throws SQLException
    {
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column)
            throws SQLException
    {
        return getColumnType(column) == Types.VARCHAR;
    }

    @Override
    public boolean isSearchable(int column)
            throws SQLException
    {
        return true;
    }

    @Override
    public boolean isCurrency(int column)
            throws SQLException
    {
        return false;
    }

    private void checkColumnIndex(int column)
            throws SQLException
    {
        if (column < 1 || column > columnNames.size()) {
            throw new SQLException("Invalid column index: " + column);
        }
    }

    @Override
    public boolean isSigned(int column)
            throws SQLException
    {
        int type = getColumnType(column);
        return type == Types.INTEGER || type == Types.BIGINT || type == Types.DOUBLE;
    }

    @Override
    public int getColumnDisplaySize(int column)
            throws SQLException
    {
        return 0;
    }

    @Override
    public int getPrecision(int column)
            throws SQLException
    {
        return 0;
    }

    @Override
    public int getScale(int column)
            throws SQLException
    {
        return 0;
    }

    @Override
    public String getTableName(int column)
            throws SQLException
    {
        return "";
    }

    @Override
    public String getSchemaName(int column)
            throws SQLException
    {
        return "";
    }

    @Override
    public String getCatalogName(int column)
            throws SQLException
    {
        return "";
    }

    @Override
    public boolean isReadOnly(int column)
            throws SQLException
    {
        return false;
    }

    @Override
    public boolean isWritable(int column)
            throws SQLException
    {
        return true;
    }

    @Override
    public boolean isDefinitelyWritable(int column)
            throws SQLException
    {
        return true;
    }

    @Override
    public <T> T unwrap(Class<T> iface)
            throws SQLException
    {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        }
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
            throws SQLException
    {
        return iface.isAssignableFrom(getClass());
    }
}
