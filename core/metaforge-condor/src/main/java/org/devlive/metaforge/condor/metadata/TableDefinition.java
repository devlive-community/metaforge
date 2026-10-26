package org.devlive.metaforge.condor.metadata;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TableDefinition
{
    private String tableName;
    private List<ColumnDefinition> columns;

    public TableDefinition(String tableName, List<ColumnDefinition> columns)
    {
        this.tableName = tableName;
        this.columns = columns;
    }

    public ColumnDefinition getColumn(String columnName)
    {
        for (ColumnDefinition column : columns) {
            if (column.getName().equals(columnName)) {
                return column;
            }
        }
        return null;
    }
}
