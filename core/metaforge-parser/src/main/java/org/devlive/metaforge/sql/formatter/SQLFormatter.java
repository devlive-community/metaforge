package org.devlive.metaforge.sql.formatter;

import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;

public abstract class SQLFormatter
{
    protected static final String INDENT = "    ";

    public String format(SQLStatement statement)
    {
        if (statement instanceof SelectStatement) {
            return new SelectFormatter().format(statement);
        }
        else {
            throw new UnsupportedOperationException("Unsupported statement: " + statement);
        }
    }
}
