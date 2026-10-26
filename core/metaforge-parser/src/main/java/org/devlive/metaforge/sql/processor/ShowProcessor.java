package org.devlive.metaforge.sql.processor;

import org.devlive.metaforge.sql.parser.SqlBaseParser;
import org.devlive.metaforge.sql.statement.ShowStatement;

public class ShowProcessor
{
    private final ExpressionProcessor expressionProcessor;

    public ShowProcessor()
    {
        this.expressionProcessor = new ExpressionProcessor();
    }

    public ShowStatement process(SqlBaseParser.ShowStatementContext ctx)
    {
        ShowStatement statement = new ShowStatement();

        if (ctx.showDatabasesStatement() != null) {
            processShowDatabases(statement, ctx.showDatabasesStatement());
        }
        else if (ctx.showTablesStatement() != null) {
            processShowTables(statement, ctx.showTablesStatement());
        }
        else if (ctx.showColumnsStatement() != null) {
            processShowColumns(statement, ctx.showColumnsStatement());
        }

        return statement;
    }

    private void processShowDatabases(ShowStatement statement,
            SqlBaseParser.ShowDatabasesStatementContext ctx)
    {
        statement.setShowType(ShowStatement.ShowType.DATABASES);
        if (ctx.STRING() != null) {
            // Remove quotes from the pattern string
            String pattern = ctx.STRING().getText();
            pattern = pattern.substring(1, pattern.length() - 1);
            statement.setPattern(pattern);
        }
    }

    private void processShowTables(ShowStatement statement,
            SqlBaseParser.ShowTablesStatementContext ctx)
    {
        statement.setShowType(ShowStatement.ShowType.TABLES);

        if (ctx.databaseName() != null) {
            statement.setDatabaseName(ctx.databaseName().getText());
        }

        if (ctx.STRING() != null) {
            // Remove quotes from the pattern string
            String pattern = ctx.STRING().getText();
            pattern = pattern.substring(1, pattern.length() - 1);
            statement.setPattern(pattern);
        }
        else if (ctx.expression() != null) {
            statement.setWhereCondition(expressionProcessor.visit(ctx.expression()));
        }
    }

    private void processShowColumns(ShowStatement statement,
            SqlBaseParser.ShowColumnsStatementContext ctx)
    {
        statement.setShowType(ShowStatement.ShowType.COLUMNS);

        if (ctx.tableName() != null) {
            statement.setTableName(ctx.tableName().getText());
        }

        if (ctx.databaseName() != null) {
            statement.setDatabaseName(ctx.databaseName().getText());
        }

        if (ctx.STRING() != null) {
            // Remove quotes from the pattern string
            String pattern = ctx.STRING().getText();
            pattern = pattern.substring(1, pattern.length() - 1);
            statement.setPattern(pattern);
        }
        else if (ctx.expression() != null) {
            statement.setWhereCondition(expressionProcessor.visit(ctx.expression()));
        }
    }
}
