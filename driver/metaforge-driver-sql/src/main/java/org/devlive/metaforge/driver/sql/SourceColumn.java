package org.devlive.metaforge.driver.sql;

/** A column as a source describes it; {@code type} is a JDBC type name such as VARCHAR or BIGINT. */
public final class SourceColumn
{
    private final String name;
    private final String type;
    private final boolean nullable;
    private final boolean key;
    private final String comment;

    public SourceColumn(String name, String type, boolean nullable, boolean key, String comment)
    {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.key = key;
        this.comment = comment == null ? "" : comment;
    }

    public static SourceColumn of(String name, String type)
    {
        return new SourceColumn(name, type, true, false, "");
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public boolean isNullable()
    {
        return nullable;
    }

    public boolean isKey()
    {
        return key;
    }

    public String getComment()
    {
        return comment;
    }
}
