package org.devlive.metaforge.driver.sql;

import org.devlive.metaforge.driver.sql.base.BaseResultSetMetaData;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Locale;

public class RowsResultSetMetaData
        extends BaseResultSetMetaData
{
    private final List<String> labels;
    private final List<String> types;

    public RowsResultSetMetaData(List<String> labels, List<String> types)
    {
        this.labels = List.copyOf(labels);
        this.types = List.copyOf(types);
    }

    private int index(int column)
            throws SQLException
    {
        if (column < 1 || column > labels.size()) {
            throw new SQLException("Column index " + column + " is out of range");
        }
        return column - 1;
    }

    /** The java.sql.Types code of a JDBC type name. */
    public static int jdbcType(String name)
    {
        switch (name == null ? "" : name.toUpperCase(Locale.ROOT)) {
            case "INTEGER":
            case "INT":
                return Types.INTEGER;
            case "BIGINT":
            case "LONG":
                return Types.BIGINT;
            case "DOUBLE":
                return Types.DOUBLE;
            case "BIT":
            case "BOOLEAN":
                return Types.BOOLEAN;
            case "TIMESTAMP":
                return Types.TIMESTAMP;
            case "BLOB":
            case "VARBINARY":
                return Types.VARBINARY;
            default:
                return Types.VARCHAR;
        }
    }

    @Override
    public int getColumnCount()
    {
        return labels.size();
    }

    @Override
    public String getColumnLabel(int column)
            throws SQLException
    {
        return labels.get(index(column));
    }

    @Override
    public String getColumnName(int column)
            throws SQLException
    {
        return labels.get(index(column));
    }

    @Override
    public String getColumnTypeName(int column)
            throws SQLException
    {
        return types.get(index(column));
    }

    @Override
    public int getColumnType(int column)
            throws SQLException
    {
        return jdbcType(types.get(index(column)));
    }

    @Override
    public String getColumnClassName(int column)
            throws SQLException
    {
        switch (getColumnType(column)) {
            case Types.INTEGER:
                return Integer.class.getName();
            case Types.BIGINT:
                return Long.class.getName();
            case Types.DOUBLE:
                return Double.class.getName();
            case Types.BOOLEAN:
                return Boolean.class.getName();
            case Types.TIMESTAMP:
                return java.sql.Timestamp.class.getName();
            case Types.VARBINARY:
                return byte[].class.getName();
            default:
                return String.class.getName();
        }
    }

    @Override
    public int isNullable(int column)
            throws SQLException
    {
        index(column);
        return columnNullable;
    }

    @Override
    public int getColumnDisplaySize(int column)
            throws SQLException
    {
        index(column);
        return 255;
    }

    @Override
    public int getPrecision(int column)
            throws SQLException
    {
        index(column);
        return 0;
    }

    @Override
    public int getScale(int column)
            throws SQLException
    {
        index(column);
        return 0;
    }

    @Override
    public String getTableName(int column)
            throws SQLException
    {
        index(column);
        return "";
    }

    @Override
    public String getSchemaName(int column)
            throws SQLException
    {
        index(column);
        return "";
    }

    @Override
    public String getCatalogName(int column)
            throws SQLException
    {
        index(column);
        return "";
    }

    @Override
    public boolean isAutoIncrement(int column)
    {
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column)
    {
        return true;
    }

    @Override
    public boolean isSearchable(int column)
    {
        return true;
    }

    @Override
    public boolean isCurrency(int column)
    {
        return false;
    }

    @Override
    public boolean isSigned(int column)
            throws SQLException
    {
        int type = getColumnType(column);
        return type == Types.INTEGER || type == Types.BIGINT || type == Types.DOUBLE;
    }

    @Override
    public boolean isReadOnly(int column)
    {
        return true;
    }

    @Override
    public boolean isWritable(int column)
    {
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column)
    {
        return false;
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
