package org.devlive.metaforge.plugin.natived.alioss;

import org.devlive.metaforge.spi.parser.SqlParser;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;
import org.devlive.metaforge.sql.statement.ShowStatement;

public class AliossParser
        extends SqlParser
{
    public AliossParser(String content)
    {
        super(content);
    }

    @Override
    public String getExecuteContext()
    {
        SQLStatement statement = this.getStatement();

        if (statement instanceof SelectStatement) {
            SelectStatement selectStatement = (SelectStatement) statement;
            return selectStatement.getFromSources().get(0).getTableName();
        }
        else if (statement instanceof ShowStatement) {
            ShowStatement showStatement = (ShowStatement) statement;
            return showStatement.getTableName();
        }
        else {
            throw new RuntimeException("Unsupported statement: " + statement);
        }
    }
}
