package org.devlive.metaforge.sql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.devlive.metaforge.sql.parser.SqlBaseLexer;
import org.devlive.metaforge.sql.parser.SqlBaseParser;
import org.devlive.metaforge.sql.statement.SQLStatement;

import java.util.ArrayList;
import java.util.List;

public class SQLParser
{
    public static SQLStatement parse(String sql)
    {
        List<SQLStatement> statements = parseMultiple(sql);
        return statements.isEmpty() ? null : statements.get(0);
    }

    public static List<SQLStatement> parseMultiple(String sql)
    {
        try {
            // 创建词法分析器和语法分析器
            // Create lexer and parser instance
            SqlBaseLexer lexer = new SqlBaseLexer(CharStreams.fromString(sql));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SqlBaseParser parser = new SqlBaseParser(tokens);

            // 使用自定义错误监听器
            // Use custom error listener
            parser.removeErrorListeners();
            parser.addErrorListener(new SQLParserErrorListener());

            // 获取解析树 - 使用 sqlStatements 而不是 statement
            // Get parse tree - use sqlStatements instead of statement
            ParseTree tree = parser.sqlStatements();

            // 访问解析树
            // Visit parse tree
            SQLVisitor visitor = new SQLVisitor();

            List<SQLStatement> statements = new ArrayList<>();

            // 遍历所有的 singleStatement
            // Traverse all singleStatement
            for (int i = 0; i < tree.getChildCount(); i++) {
                ParseTree child = tree.getChild(i);
                if (child instanceof SqlBaseParser.SingleStatementContext) {
                    SQLStatement stmt = visitor.visit(child);
                    if (stmt != null) {
                        statements.add(stmt);
                    }
                }
            }

            return statements;
        }
        catch (Exception e) {
            throw new SQLParseException("Failed to parse SQL: " + e.getMessage(), e);
        }
    }
}