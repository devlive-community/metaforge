package org.devlive.metaforge.sql.formatter;

import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.node.clause.LimitClause;
import org.devlive.metaforge.sql.node.element.OrderByElement;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.node.element.TableElement;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;

import static org.devlive.metaforge.sql.formatter.ExpressionFormatter.formatExpression;

public class SelectFormatter
        extends SQLFormatter
{
    @Override
    public String format(SQLStatement statement)
    {
        if (statement == null) {
            return "null";
        }

        SelectStatement selectStatement = (SelectStatement) statement;

        StringBuilder sb = new StringBuilder();
        String indent = INDENT;

        sb.append("SelectStatement {\n");

        // Format SELECT elements
        if (selectStatement.getSelectElements() != null && !selectStatement.getSelectElements().isEmpty()) {
            sb.append(indent).append("selectElements: [\n");
            for (SelectElement element : selectStatement.getSelectElements()) {
                sb.append(indent).append(INDENT).append("SelectElement {\n");
                if (element.getColumn() != null) {
                    sb.append(indent).append(INDENT).append(INDENT).append("column: \"").append(element.getColumn()).append("\",\n");
                }
                if (element.getAlias() != null) {
                    sb.append(indent).append(INDENT).append(INDENT).append("alias: \"").append(element.getAlias()).append("\",\n");
                }
                if (element.getExpression() != null) {
                    sb.append(indent).append(INDENT).append(INDENT).append("expression: ");
                    // 移除前面的换行，并调整缩进级别
                    String expressionStr = formatExpression(element.getExpression(), 4);
                    sb.append(expressionStr.trim()).append(",\n");
                }
                sb.append(indent).append(INDENT).append("},\n");
            }
            sb.append(indent).append("],\n");
        }

        // Format FROM sources
        if (selectStatement.getFromSources() != null && !selectStatement.getFromSources().isEmpty()) {
            sb.append(indent).append("fromSources: [\n");
            for (TableElement table : selectStatement.getFromSources()) {
                sb.append(indent).append(INDENT).append("TableElement {\n");
                if (table.getTableName() != null) {
                    sb.append(indent).append(INDENT).append(INDENT).append("tableName: \"").append(table.getTableName()).append("\",\n");
                }
                if (table.getAlias() != null) {
                    sb.append(indent).append(INDENT).append(INDENT).append("alias: \"").append(table.getAlias()).append("\",\n");
                }
                if (table.getSubquery() != null) {
                    sb.append(indent).append(INDENT).append(INDENT).append("subquery: {...},\n");
                }
                sb.append(indent).append(INDENT).append("},\n");
            }
            sb.append(indent).append("],\n");
        }

        // Format WHERE clause
        if (selectStatement.getWhereClause() != null) {
            sb.append(indent).append("whereClause: ");
            String expressionStr = formatExpression(selectStatement.getWhereClause(), 1);
            sb.append(expressionStr.trim()).append(",\n");
        }

        // Format GROUP BY elements
        if (selectStatement.getGroupByElements() != null && !selectStatement.getGroupByElements().isEmpty()) {
            sb.append(indent).append("groupByElements: [\n");
            for (Expression expr : selectStatement.getGroupByElements()) {
                sb.append(indent).append(INDENT);
                String expressionStr = formatExpression(expr, 2);
                sb.append(expressionStr.trim()).append(",\n");
            }
            sb.append(indent).append("],\n");
        }

        // Format HAVING clause
        if (selectStatement.getHavingClause() != null) {
            sb.append(indent).append("havingClause: ");
            String expressionStr = formatExpression(selectStatement.getHavingClause(), 1);
            sb.append(expressionStr.trim()).append(",\n");
        }

        // Format ORDER BY elements
        if (selectStatement.getOrderByElements() != null && !selectStatement.getOrderByElements().isEmpty()) {
            sb.append(indent).append("orderByElements: [\n");
            for (OrderByElement element : selectStatement.getOrderByElements()) {
                sb.append(indent).append(INDENT).append("OrderByElement {\n");
                if (element.getExpression() != null) {
                    sb.append(indent).append(INDENT).append(INDENT).append("expression: ");
                    String expressionStr = formatExpression(element.getExpression(), 4);
                    sb.append(expressionStr.trim()).append(",\n");
                }
                sb.append(indent).append(INDENT).append(INDENT).append("ascending: ").append(element.isAscending()).append("\n");
                sb.append(indent).append(INDENT).append("},\n");
            }
            sb.append(indent).append("],\n");
        }

        // Format LIMIT clause
        if (selectStatement.getLimitClause() != null) {
            LimitClause limit = selectStatement.getLimitClause();
            sb.append(indent).append("limitClause: {\n");
            sb.append(indent).append(INDENT).append("limit: ").append(limit.getLimit()).append(",\n");
            sb.append(indent).append(INDENT).append("offset: ").append(limit.getOffset()).append("\n");
            sb.append(indent).append("}\n");
        }

        sb.append("}");
        return sb.toString();
    }
}