package org.devlive.metaforge.spi.column;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public abstract class Column
{
    protected final ResultSet resultSet;

    public Column(ResultSet resultSet)
    {
        this.resultSet = resultSet;
    }

    public abstract Object mappingColumnData(String columnType, Integer columnIndex)
            throws SQLException;

    public abstract String mappingColumnType(String columnName)
            throws SQLException;
}
