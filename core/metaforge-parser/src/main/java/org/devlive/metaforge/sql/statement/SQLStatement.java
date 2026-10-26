package org.devlive.metaforge.sql.statement;

public abstract class SQLStatement
{
    private final StatementType type;

    public SQLStatement(StatementType type)
    {
        this.type = type;
    }

    public StatementType getType()
    {
        return type;
    }

    public enum StatementType
    {
        SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, USE, SHOW
    }
}
