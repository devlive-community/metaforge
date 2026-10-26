package org.devlive.metaforge.spi.generator.column;

import org.devlive.metaforge.spi.generator.DataType;

public class SelectColumn
        extends AbstractColumn
{
    private String alias;

    private SelectColumn(String name, DataType type)
    {
        super(name, type);
    }

    public static SelectColumn create(String name, DataType type)
    {
        return new SelectColumn(name, type);
    }

    public SelectColumn as(String alias)
    {
        this.alias = alias;
        return this;
    }

    @Override
    public String build()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("`").append(name).append("`");

        if (alias != null) {
            sql.append(" AS `").append(alias).append("`");
        }

        return sql.toString();
    }
}
