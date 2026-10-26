package org.devlive.metaforge.spi.column;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

public class JdbcColumn
        extends Column
{
    public JdbcColumn(ResultSet resultSet)
    {
        super(resultSet);
    }

    @Override
    public Object mappingColumnData(String columnType, Integer columnIndex)
            throws SQLException
    {
        // Obtain the value of the corresponding type based on the data returned in the query result
        if (columnType.equalsIgnoreCase(JdbcColumnType.INTEGER.name())
                || columnType.equalsIgnoreCase(JdbcColumnType.INT.name())) {
            return resultSet.getInt(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.TINYINT.name())) {
            return resultSet.getByte(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.DOUBLE.name())) {
            return resultSet.getDouble(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.REAL.name())) {
            return resultSet.getFloat(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.BIGINT.name())) {
            return resultSet.getLong(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.SMALLINT.name())) {
            return resultSet.getShort(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.DECIMAL.name())) {
            return resultSet.getBigDecimal(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.BLOB.name())) {
            return resultSet.getBytes(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.VARCHAR.name())
                || String.class.getSimpleName().equals(columnType)) {
            return resultSet.getString(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.BIT.name())) {
            return resultSet.getBoolean(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.DATE.name())) {
            return resultSet.getDate(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.TIME.name())) {
            return resultSet.getTime(columnIndex);
        }
        else if (columnType.equalsIgnoreCase(JdbcColumnType.TIMESTAMP.name())) {
            return resultSet.getTimestamp(columnIndex);
        }
        else {
            return resultSet.getObject(columnIndex);
        }
    }

    @Override
    public String mappingColumnType(String columnName)
            throws SQLException
    {
        // Obtain the value of the corresponding type based on the data returned in the query result
        if (columnName.equalsIgnoreCase(JdbcColumnType.INTEGER.name())
                || columnName.equalsIgnoreCase(JdbcColumnType.INT.name())) {
            return Integer.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.TINYINT.name())
                || columnName.equalsIgnoreCase(JdbcColumnType.BLOB.name())) {
            return Byte.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.DOUBLE.name())) {
            return Double.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.REAL.name())) {
            return Float.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.BIGINT.name())) {
            return Long.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.SMALLINT.name())) {
            return Short.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.DECIMAL.name())) {
            return BigDecimal.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.BIT.name())) {
            return Boolean.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.DATE.name())) {
            return Date.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.TIME.name())) {
            return Time.class.getSimpleName();
        }
        else if (columnName.equalsIgnoreCase(JdbcColumnType.TIMESTAMP.name())) {
            return Timestamp.class.getSimpleName();
        }
        else {
            return String.class.getSimpleName();
        }
    }
}
