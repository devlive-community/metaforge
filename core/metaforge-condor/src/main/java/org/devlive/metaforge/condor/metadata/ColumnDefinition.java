package org.devlive.metaforge.condor.metadata;

import lombok.Getter;
import org.devlive.metaforge.condor.DataType;

@Getter
public class ColumnDefinition
{
    private String name;
    private DataType type;
    private boolean isPrimaryKey;
    private boolean isNullable;

    public ColumnDefinition(String name, DataType type, boolean isPrimaryKey, boolean isNullable)
    {
        this.name = name;
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isNullable = isNullable;
    }
}
