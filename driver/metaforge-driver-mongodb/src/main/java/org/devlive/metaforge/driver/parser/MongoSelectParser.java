package org.devlive.metaforge.driver.parser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.bson.Document;
import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.node.clause.LimitClause;
import org.devlive.metaforge.sql.node.element.OrderByElement;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.node.element.TableElement;
import org.devlive.metaforge.sql.statement.SelectStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Getter
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
public class MongoSelectParser
        extends MongoParser
{
    private final Map<String, String> fieldAliasMap = new HashMap<>();
    private final Map<String, String> aliasToFieldMap = new HashMap<>();

    public MongoSelectParser(SelectStatement statement)
    {
        parseSelectStatement(statement);
    }

    // Parse SELECT statement
    // 解析SELECT语句
    public void parseSelectStatement(SelectStatement select)
    {
        List<SelectElement> elements = select.getSelectElements();
        if (elements != null && elements.size() == 1) {
            SelectElement element = elements.get(0);
            if (element.getExpression() != null &&
                    element.getExpression().getType() == Expression.ExpressionType.FUNCTION &&
                    "VERSION".equalsIgnoreCase(element.getExpression().getValue().toString())) {
                this.query = new Document("buildInfo", 1);
                return;
            }
        }

        // Get collection name first
        parseFromClause(select.getFromSources());

        // Parse select elements to set fields
        parseSelectElements(select.getSelectElements());

        // Initialize an aggregation pipeline
        List<Document> pipeline = new ArrayList<>();

        // Add $match stage for WHERE conditions
        if (select.getWhereClause() != null) {
            Object queryResult = parseExpression(select.getWhereClause());
            Document matchStage = new Document("$match",
                    queryResult instanceof Document ? queryResult : new Document("$eq", queryResult));
            pipeline.add(matchStage);
        }

        // Add $project stage for field selection
        // Add $group stage if GROUP BY exists
        if (select.getGroupByElements() != null && !select.getGroupByElements().isEmpty()) {
            Document groupStage = parseGroupByClause(select.getGroupByElements(), select.getSelectElements());
            pipeline.add(new Document("$group", groupStage));
        }
        // If no GROUP BY, add normal $project stage
        else if (fields != null && !fields.isEmpty() &&
                !(fields.size() == 1 && fields.get(0).equals("*"))) {
            Document projectStage = new Document();
            projectStage.put("_id", 0);

            // Create field mappings in $project stage
            for (SelectElement element : select.getSelectElements()) {
                String originalField = element.getColumn() != null ?
                        element.getColumn() :
                        element.getExpression().getValue().toString();

                String alias = element.getAlias();
                if (alias != null) {
                    projectStage.put(alias, "$" + originalField);
                }
                else {
                    projectStage.put(originalField, 1);
                }
            }
            pipeline.add(new Document("$project", projectStage));
        }

        // Add $sort stage if ORDER BY exists
        if (select.getOrderByElements() != null && !select.getOrderByElements().isEmpty()) {
            Document sortStage = new Document("$sort", parseOrderByElements(select.getOrderByElements()));
            pipeline.add(sortStage);
        }

        // Add $skip and $limit stages if present
        LimitClause limitClause = select.getLimitClause();
        if (limitClause != null) {
            if (limitClause.getOffset() > 0) {
                pipeline.add(new Document("$skip", (int) limitClause.getOffset()));
            }
            if (limitClause.getLimit() >= 0) {
                pipeline.add(new Document("$limit", (int) limitClause.getLimit()));
            }
        }

        // Set the final query
        this.query = new Document("aggregate", this.collection)
                .append("pipeline", pipeline)
                .append("cursor", new Document());
    }

    // Parse SELECT elements to field list
    // 解析SELECT元素到字段列表
    private void parseSelectElements(List<SelectElement> elements)
    {
        this.fields = new ArrayList<>();
        if (elements != null) {
            for (SelectElement element : elements) {
                String field;
                // Get field name (from column name or expression)
                if (element.getColumn() != null) {
                    field = element.getColumn();
                }
                else if (element.getExpression() != null) {
                    Expression expr = element.getExpression();
                    if (expr.getType() == Expression.ExpressionType.FUNCTION &&
                            "VERSION".equalsIgnoreCase(expr.getValue().toString())) {
                        field = "version";
                    }
                    else {
                        field = parseExpression(expr).toString();
                    }
                }
                else {
                    continue;
                }

                // Handle alias mapping
                if (element.getAlias() != null) {
                    fieldAliasMap.put(field, element.getAlias());
                    aliasToFieldMap.put(element.getAlias(), field);
                    fields.add(element.getAlias());
                }
                else {
                    fields.add(field);
                }
            }
        }
    }

    // Parse FROM clause to get collection name
    // 解析FROM子句获取集合名称
    private void parseFromClause(List<TableElement> fromSources)
    {
        if (fromSources != null && !fromSources.isEmpty()) {
            TableElement mainTable = fromSources.get(0);
            this.collection = mainTable.getTableName();

            // MongoDB doesn't support JOINs
            // MongoDB不支持JOIN操作
            if (mainTable.getJoins() != null && !mainTable.getJoins().isEmpty()) {
                throw new IllegalArgumentException("MongoDB does not support JOIN operations");
            }
        }
    }

    private Object parseExpression(Expression expr)
    {
        if (expr == null) {
            return null;
        }

        switch (expr.getType()) {
            case LITERAL:
                return parseValue(expr.getValue().toString());

            case COLUMN_REFERENCE:
                return expr.getValue().toString();

            case BINARY_OP:
                String operator = expr.getValue().toString();
                List<Expression> children = expr.getChildren();

                // Handle logical operators (AND, OR)
                if ("AND".equalsIgnoreCase(operator) || "OR".equalsIgnoreCase(operator)) {
                    List<Document> conditions = new ArrayList<>();
                    for (Expression child : children) {
                        Object result = parseExpression(child);
                        if (result instanceof Document) {
                            conditions.add((Document) result);
                        }
                    }
                    return new Document(operator.equalsIgnoreCase("AND") ? "$and" : "$or", conditions);
                }

                // Handle comparison operators
                if (children != null && children.size() == 2) {
                    Expression left = children.get(0);
                    Expression right = children.get(1);

                    String field = parseExpression(left).toString();
                    Object value = parseExpression(right);

                    Document condition = new Document();
                    switch (operator) {
                        case "=":
                            condition.put(field, value);
                            break;
                        case ">":
                            condition.put(field, new Document("$gt", value));
                            break;
                        case "<":
                            condition.put(field, new Document("$lt", value));
                            break;
                        case ">=":
                            condition.put(field, new Document("$gte", value));
                            break;
                        case "<=":
                            condition.put(field, new Document("$lte", value));
                            break;
                        case "!=":
                            condition.put(field, new Document("$ne", value));
                            break;
                        case "LIKE":
                            String pattern = value.toString().replace("%", ".*");
                            condition.put(field, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                            break;
                        case "IN":
                            condition.put(field, new Document("$in", value));
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported operator: " + operator);
                    }
                    return condition;
                }

                throw new IllegalArgumentException("Invalid binary expression structure");

            case FUNCTION:
                if ("VERSION".equalsIgnoreCase(expr.getValue().toString())) {
                    return new Document("$buildInfo", 1);
                }

                throw new IllegalArgumentException("Unsupported function: " + expr.getValue());

            default:
                throw new IllegalArgumentException("Unsupported expression type: " + expr.getType());
        }
    }

    // Parse ORDER BY elements to MongoDB sort document
    // 解析ORDER BY元素到MongoDB排序文档
    private Document parseOrderByElements(List<OrderByElement> elements)
    {
        Document orderBy = new Document();
        for (OrderByElement element : elements) {
            String field = element.getExpression().getValue().toString();
            orderBy.put(field, element.isAscending() ? 1 : -1);
        }
        return orderBy;
    }

    // Parse string value to appropriate type
    // 将字符串值解析为适当的类型
    private Object parseValue(String value)
    {
        value = value.trim();

        // Remove quotes if present
        // 如果有引号则移除
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }

        // Try parsing as number
        // 尝试解析为数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            else {
                return Long.parseLong(value);
            }
        }
        catch (NumberFormatException e) {
            // Return as string if not a number
            // 如果不是数字则返回字符串
            return value;
        }
    }

    private Document parseGroupByClause(List<Expression> groupByColumns, List<SelectElement> selectElements)
    {
        Document groupStage = new Document();

        // Handle _id field for grouping
        if (groupByColumns.size() == 1 && groupByColumns.get(0).getValue().equals("_id")) {
            groupStage.put("_id", "$" + groupByColumns.get(0).getValue());
        }
        else {
            // Multiple group by columns
            Document idDoc = new Document();
            for (Expression expr : groupByColumns) {
                String field = expr.getValue().toString();
                idDoc.put(field, "$" + field);
            }
            groupStage.put("_id", idDoc);
        }

        // Handle aggregation functions in SELECT clause
        for (SelectElement element : selectElements) {
            if (element.getExpression() != null) {
                Expression expr = element.getExpression();
                if (expr.getType() == Expression.ExpressionType.FUNCTION) {
                    String functionName = expr.getValue().toString().toUpperCase();
                    String field = expr.getChildren().get(0).getValue().toString();
                    String alias = element.getAlias() != null ? element.getAlias() : functionName + "_" + field;

                    switch (functionName) {
                        case "COUNT":
                            groupStage.put(alias, new Document("$sum", 1));
                            break;
                        case "SUM":
                            groupStage.put(alias, new Document("$sum", "$" + field));
                            break;
                        case "AVG":
                            groupStage.put(alias, new Document("$avg", "$" + field));
                            break;
                        case "MIN":
                            groupStage.put(alias, new Document("$min", "$" + field));
                            break;
                        case "MAX":
                            groupStage.put(alias, new Document("$max", "$" + field));
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported aggregation function: " + functionName);
                    }
                }
                else {
                    // Handle non-aggregated fields that are part of GROUP BY
                    String field = expr.getValue().toString();
                    if (isFieldInGroupBy(field, groupByColumns)) {
                        groupStage.put(field, new Document("$first", "$" + field));
                    }
                }
            }
            else if (element.getColumn() != null) {
                // Handle simple columns that are part of GROUP BY
                String field = element.getColumn();
                if (isFieldInGroupBy(field, groupByColumns)) {
                    groupStage.put(field, new Document("$first", "$" + field));
                }
            }
        }

        return groupStage;
    }

    private boolean isFieldInGroupBy(String field, List<Expression> groupByColumns)
    {
        return groupByColumns.stream()
                .anyMatch(expr -> expr.getValue().toString().equals(field));
    }
}
