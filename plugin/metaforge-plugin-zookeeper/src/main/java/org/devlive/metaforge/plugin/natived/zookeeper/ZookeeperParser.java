package org.devlive.metaforge.plugin.natived.zookeeper;

import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.spi.parser.SqlParser;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;
import org.devlive.metaforge.sql.statement.ShowStatement;

public class ZookeeperParser
        extends SqlParser
{
    public ZookeeperParser(String content)
    {
        super(content);
    }

    @Override
    public String getExecuteContext()
    {
        SQLStatement statement = this.getStatement();

        if (statement instanceof SelectStatement) {
            SelectStatement selectStatement = (SelectStatement) statement;
            String tableName = selectStatement.getFromSources().get(0).getTableName();

            if (StringUtils.isEmpty(tableName)) {
                return ZookeeperPathConvert.start;
            }
            return ZookeeperPathConvert.toPath(tableName);
        }
        else if (statement instanceof ShowStatement) {
            ShowStatement showStatement = (ShowStatement) statement;
            return ZookeeperPathConvert.toPath(showStatement.getTableName());
        }
        else {
            throw new RuntimeException("Unsupported statement: " + statement);
        }
    }
}
