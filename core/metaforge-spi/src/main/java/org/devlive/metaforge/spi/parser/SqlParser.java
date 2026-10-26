package org.devlive.metaforge.spi.parser;

import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.statement.SQLStatement;

public class SqlParser
        implements Parser
{
    private final String content;

    public SqlParser(String content)
    {
        this.content = content;
    }

    @Override
    public SQLStatement getStatement()
    {
        return SQLParser.parse(content.trim());
    }

    @Override
    public String getExecuteContext()
    {
        return null;
    }
}
