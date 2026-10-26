package org.devlive.metaforge.spi.generator.column;

import org.devlive.metaforge.spi.generator.DataType;

public abstract class AbstractColumn
        implements Column
{
    protected final String name;
    protected final DataType type;
    protected Integer length;
    protected Integer precision;
    protected Integer scale;
    protected boolean nullable = true;
    protected String defaultValue;
    protected String comment;
    protected String charset;
    protected String collate;

    protected AbstractColumn(String name, DataType type)
    {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public DataType getType()
    {
        return type;
    }

    public AbstractColumn length(int length)
    {
        this.length = length;
        return this;
    }

    public AbstractColumn precision(int precision, int scale)
    {
        this.precision = precision;
        this.scale = scale;
        return this;
    }

    public AbstractColumn notNull()
    {
        this.nullable = false;
        return this;
    }

    public AbstractColumn defaultValue(String value)
    {
        this.defaultValue = value;
        return this;
    }

    public AbstractColumn comment(String comment)
    {
        this.comment = comment;
        return this;
    }

    public AbstractColumn charset(String charset)
    {
        this.charset = charset;
        return this;
    }

    public AbstractColumn collate(String collate)
    {
        this.collate = collate;
        return this;
    }

    protected StringBuilder buildBasicSQL()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("`").append(name).append("` ");

        if (length != null) {
            sql.append(type.withLength(length));
        }
        else if (precision != null) {
            sql.append(type.withPrecision(precision, scale));
        }
        else {
            sql.append(type.getValue());
        }

        if (charset != null) {
            sql.append(" CHARACTER SET ").append(charset);
        }

        if (collate != null) {
            sql.append(" COLLATE ").append(collate);
        }

        if (!nullable) {
            sql.append(" NOT NULL");
        }

        if (defaultValue != null) {
            if (defaultValue.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                sql.append(" DEFAULT ").append(defaultValue);
            }
            else {
                sql.append(" DEFAULT '").append(defaultValue).append("'");
            }
        }

        return sql;
    }
}
