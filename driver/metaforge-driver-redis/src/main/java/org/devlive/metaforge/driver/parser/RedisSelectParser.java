package org.devlive.metaforge.driver.parser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.node.element.TableElement;
import org.devlive.metaforge.sql.statement.SelectStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
public class RedisSelectParser
        extends RedisParser
{
    public RedisSelectParser(SelectStatement statement)
    {
        parseSelectStatement(statement);
    }

    private void parseSelectStatement(SelectStatement select)
    {
        this.query = new HashMap<>();
        this.fields = new ArrayList<>();

        if (isVersionQuery(select)) {
            this.query.put("serverInfo", true);
            return;
        }

        List<TableElement> fromSources = select.getFromSources();
        if (fromSources != null && !fromSources.isEmpty()) {
            TableElement mainTable = fromSources.get(0);
            this.collection = mainTable.getTableName();
        }

        List<SelectElement> selectElements = select.getSelectElements();
        if (selectElements != null) {
            for (SelectElement element : selectElements) {
                if (element.getExpression() != null) {
                    String fieldName = element.getExpression().getValue().toString();
                    if (element.getAlias() != null) {
                        fieldName = element.getAlias();
                    }
                    fields.add(fieldName);
                }
            }
        }

        if (select.getWhereClause() != null) {
            this.filter = new HashMap<>();
            parseWhereExpression(select.getWhereClause());
        }
    }

    private boolean isVersionQuery(SelectStatement select)
    {
        List<SelectElement> elements = select.getSelectElements();
        if (elements != null && elements.size() == 1) {
            SelectElement element = elements.get(0);
            if (element.getExpression() != null) {
                Expression expr = element.getExpression();
                if (expr.getType() == Expression.ExpressionType.FUNCTION &&
                        "VERSION".equalsIgnoreCase(expr.getValue().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void parseWhereExpression(Expression expression)
    {
        if (expression == null || filter == null) {
            return;
        }
        filter.put("condition", expression.getValue().toString());
    }
}
