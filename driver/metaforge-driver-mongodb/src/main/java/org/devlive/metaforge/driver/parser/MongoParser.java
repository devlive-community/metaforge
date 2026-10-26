package org.devlive.metaforge.driver.parser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;
import org.devlive.metaforge.sql.statement.ShowStatement;

import java.util.List;

@Getter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class MongoParser
{
    protected Document filter;
    protected List<String> fields;
    protected String command;
    protected Document query;
    protected String collection;
    protected ShowStatement.ShowType showType;

    @Setter
    protected String database;

    // Parse SQL statement
    // 解析SQL语句
    public static MongoParser createParser(String sql)
    {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }

        SQLStatement statement = SQLParser.parse(sql.trim());
        if (statement instanceof SelectStatement) {
            return new MongoSelectParser((SelectStatement) statement);
        }
        else if (statement instanceof ShowStatement) {
            return new MongoShowParser((ShowStatement) statement);
        }
        throw new IllegalArgumentException("Unsupported SQL operation: " + sql);
    }
}
