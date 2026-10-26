package org.devlive.metaforge.driver.parser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;
import org.devlive.metaforge.sql.statement.ShowStatement;

import java.util.List;
import java.util.Map;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class RedisParser
{
    protected Map<String, Object> filter;
    protected List<String> fields;
    protected String command;
    protected Map<String, Object> query;
    protected String collection;
    protected ShowStatement.ShowType showType;
    protected String database;

    public static RedisParser createParser(String sql)
    {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }

        SQLStatement statement = SQLParser.parse(sql.trim());
        if (statement instanceof SelectStatement) {
            return new RedisSelectParser((SelectStatement) statement);
        }
        else if (statement instanceof ShowStatement) {
            return new RedisShowParser((ShowStatement) statement);
        }
        throw new IllegalArgumentException("Unsupported SQL operation: " + sql);
    }

    public Map<String, Object> getFilter()
    {
        return filter;
    }

    public List<String> getFields()
    {
        return fields;
    }

    public String getCommand()
    {
        return command;
    }

    public Map<String, Object> getQuery()
    {
        return query;
    }

    public String getCollection()
    {
        return collection;
    }

    public ShowStatement.ShowType getShowType()
    {
        return showType;
    }

    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }
}
