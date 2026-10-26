package org.devlive.metaforge.spi.generator.column;

import org.devlive.metaforge.spi.generator.DataType;

public class CreateColumn
        extends AbstractColumn
{
    private boolean autoIncrement;

    protected CreateColumn(String name, DataType type)
    {
        super(name, type);
    }

    public static CreateColumn create(String name, DataType type)
    {
        return new CreateColumn(name, type);
    }

    public CreateColumn autoIncrement()
    {
        this.autoIncrement = true;
        return this;
    }

    @Override
    public String build()
    {
        StringBuilder sql = buildBasicSQL();

        if (autoIncrement) {
            sql.append(" AUTO_INCREMENT");
        }

        if (comment != null) {
            sql.append(" COMMENT '").append(comment).append("'");
        }

        return sql.toString();
    }
}
