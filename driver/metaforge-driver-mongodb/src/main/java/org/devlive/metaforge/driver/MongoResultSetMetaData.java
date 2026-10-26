package org.devlive.metaforge.driver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.Document;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings(value = {"NP_NONNULL_RETURN_VIOLATION", "EI_EXPOSE_REP2"})
public class MongoResultSetMetaData
        implements ResultSetMetaData
{
    private final List<String> columnNames;
    private final Map<String, Object> sampleRow;

    // Constructor
    // 构造函数
    public MongoResultSetMetaData(List<String> columnNames, Document sampleRow)
    {
        this.columnNames = columnNames;
        this.sampleRow = sampleRow;
    }

    // Get number of columns
    // 获取列数
    @Override
    public int getColumnCount()
            throws SQLException
    {
        return columnNames.size();
    }

    // Get column name
    // 获取列名
    @Override
    public String getColumnName(int column)
            throws SQLException
    {
        checkColumnIndex(column);
        return columnNames.get(column - 1);
    }

    // Get column label
    // 获取列标签
    @Override
    public String getColumnLabel(int column)
            throws SQLException
    {
        return getColumnName(column);
    }

    // Get column type
    // 获取列类型
    @Override
    public int getColumnType(int column)
            throws SQLException
    {
        checkColumnIndex(column);

        String columnName = columnNames.get(column - 1);
        Object value = sampleRow.get(columnName);
        return MongoTypeHelper.getJdbcType(value);
    }

    // Get column type name
    // 获取列类型名称
    @Override
    public String getColumnTypeName(int column)
            throws SQLException
    {
        checkColumnIndex(column);

        String columnName = columnNames.get(column - 1);
        Object value = sampleRow.get(columnName);
        return MongoTypeHelper.getTypeName(value);
    }

    // Get column class name
    // 获取列的Java类名
    @Override
    public String getColumnClassName(int column)
            throws SQLException
    {
        checkColumnIndex(column);

        String columnName = columnNames.get(column - 1);
        Object value = sampleRow.get(columnName);
        return MongoTypeHelper.getJavaClassName(value);
    }

    // Check if column is nullable
    // 检查列是否可为空
    @Override
    public int isNullable(int column)
            throws SQLException
    {
        return columnNullable;
    }

    // Check if column is auto increment
    // 检查列是否自动递增
    @Override
    public boolean isAutoIncrement(int column)
            throws SQLException
    {
        String columnName = getColumnName(column);
        return columnName.equals("_id");
    }

    // Check if column is case sensitive
    // 检查列是否大小写敏感
    @Override
    public boolean isCaseSensitive(int column)
            throws SQLException
    {
        return getColumnType(column) == Types.VARCHAR;
    }

    // Check if column is searchable
    // 检查列是否可搜索
    @Override
    public boolean isSearchable(int column)
            throws SQLException
    {
        return true;
    }

    // Check if column is currency
    // 检查列是否货币类型
    @Override
    public boolean isCurrency(int column)
            throws SQLException
    {
        return false;
    }

    // Validate column index
    // 验证列索引
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
