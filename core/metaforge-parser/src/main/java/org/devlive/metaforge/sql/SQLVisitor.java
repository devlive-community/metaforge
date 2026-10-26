package org.devlive.metaforge.sql;

import com.google.common.collect.Lists;
import org.antlr.v4.runtime.RuleContext;
import org.devlive.metaforge.sql.node.ColumnConstraint;
import org.devlive.metaforge.sql.node.ConstraintType;
import org.devlive.metaforge.sql.node.DataType;
import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.node.TableConstraint;
import org.devlive.metaforge.sql.node.clause.ForeignKeyClause;
import org.devlive.metaforge.sql.node.clause.JoinClause;
import org.devlive.metaforge.sql.node.clause.LimitClause;
import org.devlive.metaforge.sql.node.element.ColumnElement;
import org.devlive.metaforge.sql.node.element.OrderByElement;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.node.element.TableElement;
import org.devlive.metaforge.sql.node.option.ReferenceOption;
import org.devlive.metaforge.sql.node.option.TableOption;
import org.devlive.metaforge.sql.parser.SqlBaseBaseVisitor;
import org.devlive.metaforge.sql.parser.SqlBaseParser;
import org.devlive.metaforge.sql.processor.ExpressionProcessor;
import org.devlive.metaforge.sql.processor.ShowProcessor;
import org.devlive.metaforge.sql.statement.CreateDatabaseStatement;
import org.devlive.metaforge.sql.statement.CreateTableStatement;
import org.devlive.metaforge.sql.statement.DeleteStatement;
import org.devlive.metaforge.sql.statement.DropDatabaseStatement;
import org.devlive.metaforge.sql.statement.DropTableStatement;
import org.devlive.metaforge.sql.statement.InsertStatement;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;
import org.devlive.metaforge.sql.statement.UpdateStatement;
import org.devlive.metaforge.sql.statement.UseDatabaseStatement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SQLVisitor
        extends SqlBaseBaseVisitor<SQLStatement>
{
    @Override
    public SQLStatement visitSingleStatement(SqlBaseParser.SingleStatementContext ctx)
    {
        return visit(ctx.statement());
    }

    @Override
    public SQLStatement visitStatement(SqlBaseParser.StatementContext ctx)
    {
        if (ctx.selectStatement() != null) {
            return visitSelectStatement(ctx.selectStatement());
        }
        else if (ctx.insertStatement() != null) {
            return visitInsertStatement(ctx.insertStatement());
        }
        else if (ctx.updateStatement() != null) {
            return visitUpdateStatement(ctx.updateStatement());
        }
        else if (ctx.deleteStatement() != null) {
            return visitDeleteStatement(ctx.deleteStatement());
        }
        else if (ctx.createStatement() != null) {
            return visitCreateStatement(ctx.createStatement());
        }
        else if (ctx.alterStatement() != null) {
            return visitAlterStatement(ctx.alterStatement());
        }
        else if (ctx.dropStatement() != null) {
            return visitDropStatement(ctx.dropStatement());
        }
        else if (ctx.useStatement() != null) {
            return visitUseStatement(ctx.useStatement());
        }
        else if (ctx.showStatement() != null) {
            return visitShowStatement(ctx.showStatement());
        }
        return null;
    }

    @Override
    public SQLStatement visitCreateDatabaseStatement(SqlBaseParser.CreateDatabaseStatementContext ctx)
    {
        String databaseName = ctx.databaseName().getText();
        boolean ifNotExists = ctx.EXISTS() != null;
        return new CreateDatabaseStatement(databaseName, ifNotExists);
    }

    @Override
    public SQLStatement visitSelectStatement(SqlBaseParser.SelectStatementContext ctx)
    {
        SelectStatement statement = new SelectStatement();

        // Parse SELECT elements
        if (ctx.queryExpression().queryTerm().queryPrimary().querySpecification() != null) {
            SqlBaseParser.QuerySpecificationContext querySpec =
                    ctx.queryExpression().queryTerm().queryPrimary().querySpecification();

            statement.setSelectElements(processSelectElements(querySpec.selectElements()));

            // Parse FROM clause
            if (querySpec.fromClause() != null) {
                statement.setFromSources(processFromClause(querySpec.fromClause()));
            }

            // Parse WHERE clause
            if (querySpec.whereClause() != null) {
                statement.setWhereClause(processExpression(querySpec.whereClause().expression()));
            }

            // Parse GROUP BY clause
            if (querySpec.groupByClause() != null) {
                statement.setGroupByElements(visitGroupByElements(querySpec.groupByClause()));
            }

            // Parse HAVING clause
            if (querySpec.havingClause() != null) {
                statement.setHavingClause(processExpression(querySpec.havingClause().expression()));
            }
        }

        // Parse ORDER BY clause
        if (ctx.orderByClause() != null) {
            statement.setOrderByElements(visitOrderByElements(ctx.orderByClause()));
        }

        // Parse LIMIT clause
        if (ctx.limitClause() != null) {
            statement.setLimitClause(processLimitClause(ctx.limitClause()));
        }

        return statement;
    }

    @Override
    public SQLStatement visitQueryExpression(SqlBaseParser.QueryExpressionContext ctx)
    {
        return visit(ctx.queryTerm());
    }

    @Override
    public SQLStatement visitQueryTerm(SqlBaseParser.QueryTermContext ctx)
    {
        return visit(ctx.queryPrimary());
    }

    @Override
    public SQLStatement visitQueryPrimary(SqlBaseParser.QueryPrimaryContext ctx)
    {
        if (ctx.querySpecification() != null) {
            return visit(ctx.querySpecification());
        }
        else if (ctx.queryExpression() != null) {
            return visit(ctx.queryExpression());
        }
        return null;
    }

    @Override
    public SQLStatement visitInsertStatement(SqlBaseParser.InsertStatementContext ctx)
    {
        String tableName = ctx.tableName().getText();
        boolean orReplace = ctx.REPLACE() != null;

        // Parse column names if present
        List<String> columns = Lists.newArrayList();
        if (ctx.columnName() != null && !ctx.columnName().isEmpty()) {
            columns = ctx.columnName()
                    .stream()
                    .map(RuleContext::getText)
                    .collect(Collectors.toList());
        }

        // Handle VALUES case
        List<List<Expression>> values = Lists.newArrayList();
        List<List<Object>> simpleValues = Lists.newArrayList();
        SelectStatement select = null;

        if (ctx.insertValuesConstructor() != null && !ctx.insertValuesConstructor().isEmpty()) {
            for (SqlBaseParser.InsertValuesConstructorContext valueCtx : ctx.insertValuesConstructor()) {
                List<Expression> row = valueCtx.value()
                        .stream()
                        .map(SqlBaseParser.ValueContext::expression)
                        .map(this::processExpression)
                        .collect(Collectors.toList());
                simpleValues.add(
                        row.stream()
                                .map(Expression::getValue)
                                .collect(Collectors.toList())
                );
                values.add(row);
            }
        }
        // Handle SELECT case
        else if (ctx.selectStatement() != null) {
            select = (SelectStatement) visitSelectStatement(ctx.selectStatement());
        }

        return new InsertStatement(tableName, orReplace, columns, values, simpleValues, select);
    }

    @Override
    public SQLStatement visitUpdateStatement(SqlBaseParser.UpdateStatementContext ctx)
    {
        String tableName = ctx.tableName().getText();

        Map<String, Expression> setValues = new LinkedHashMap<>();
        for (SqlBaseParser.UpdateElementContext elementCtx : ctx.updateElement()) {
            String columnName = elementCtx.columnName().getText();
            Expression value = processExpression(elementCtx.expression());
            setValues.put(columnName, value);
        }

        Expression whereClause = null;
        if (ctx.whereClause() != null) {
            whereClause = processExpression(ctx.whereClause().expression());
        }

        return new UpdateStatement(tableName, setValues, whereClause);
    }

    @Override
    public SQLStatement visitDeleteStatement(SqlBaseParser.DeleteStatementContext ctx)
    {
        String tableName = ctx.tableName().getText();

        Expression whereClause = null;
        if (ctx.whereClause() != null) {
            whereClause = processExpression(ctx.whereClause().expression());
        }

        return new DeleteStatement(tableName, whereClause);
    }

    @Override
    public SQLStatement visitCreateStatement(SqlBaseParser.CreateStatementContext ctx)
    {
        if (ctx.createDatabaseStatement() != null) {
            return visitCreateDatabaseStatement(ctx.createDatabaseStatement());
        }

        if (ctx.createTableStatement() != null) {
            return visitCreateTableStatement(ctx.createTableStatement());
        }

        return null;
    }

    @Override
    public SQLStatement visitCreateTableStatement(SqlBaseParser.CreateTableStatementContext ctx)
    {
        // Parse basic table information
        String tableName = ctx.tableName().getText();
        boolean isTemporary = ctx.TEMP() != null || ctx.TEMPORARY() != null;
        boolean ifNotExists = ctx.IF() != null && ctx.NOT() != null && ctx.EXISTS() != null;

        // Parse table elements
        List<TableElement> elements = new ArrayList<>();
        for (SqlBaseParser.TableElementContext elementCtx : ctx.tableElement()) {
            if (elementCtx.columnDefinition() != null) {
                elements.add(processColumnDefinition(elementCtx.columnDefinition()));
            }
            else if (elementCtx.tableConstraint() != null) {
                elements.add(processTableConstraint(elementCtx.tableConstraint()));
            }
        }

        // Parse table options
        List<TableOption> options = new ArrayList<>();
        if (ctx.tableOptions() != null) {
            for (SqlBaseParser.TableOptionContext optionCtx : ctx.tableOptions().tableOption()) {
                if (optionCtx.getChildCount() >= 3 && optionCtx.getChild(1).getText().equals("=")) {
                    String name = null;
                    String value = null;

                    if (optionCtx.ENGINE() != null && optionCtx.STRING() != null) {
                        name = "ENGINE";
                        value = unquoteString(optionCtx.STRING().getText());
                    }
                    else if (optionCtx.CHARSET() != null && optionCtx.STRING() != null) {
                        name = "CHARSET";
                        value = unquoteString(optionCtx.STRING().getText());
                    }
                    else if (optionCtx.COLLATE() != null && optionCtx.STRING() != null) {
                        name = "COLLATE";
                        value = unquoteString(optionCtx.STRING().getText());
                    }
                    else if (optionCtx.AUTO_INCREMENT() != null && optionCtx.INTEGER_VALUE() != null) {
                        name = "AUTO_INCREMENT";
                        value = optionCtx.INTEGER_VALUE().getText();
                    }
                    else if (optionCtx.COMMENT() != null && optionCtx.STRING() != null) {
                        name = "COMMENT";
                        value = unquoteString(optionCtx.STRING().getText());
                    }

                    if (name != null && value != null) {
                        options.add(new TableOption(name, value));
                    }
                }
                // 处理Flink SQL的WITH子句
                else if (optionCtx.WITH() != null && optionCtx.tableProperty() != null) {
                    for (SqlBaseParser.TablePropertyContext propCtx : optionCtx.tableProperty()) {
                        String name = null;
                        String value = null;

                        if (propCtx.STRING() != null && propCtx.STRING().size() >= 2) {
                            // 处理 'key' = 'value' 形式
                            name = unquoteString(propCtx.STRING(0).getText());
                            value = unquoteString(propCtx.STRING(1).getText());
                        }
                        else if (propCtx.identifier() != null && propCtx.STRING() != null) {
                            // 处理 identifier = 'value' 形式
                            name = propCtx.identifier().getText();
                            value = unquoteString(propCtx.getText());
                        }

                        if (name != null && value != null) {
                            options.add(new TableOption(name, value));
                        }
                    }
                }
            }
        }

        return new CreateTableStatement(
                tableName,
                isTemporary,
                ifNotExists,
                elements,
                options
        );
    }

    private ColumnElement processColumnDefinition(SqlBaseParser.ColumnDefinitionContext ctx)
    {
        // Parse column name
        String columnName = ctx.columnName().getText();

        // Parse data type
        DataType dataType = processDataType(ctx.dataType());

        // Parse column constraints
        List<ColumnConstraint> constraints = new ArrayList<>();
        for (SqlBaseParser.ColumnConstraintContext constraintCtx : ctx.columnConstraint()) {
            String constraintName = constraintCtx.constraintName() != null ?
                    constraintCtx.constraintName().getText() : null;

            ConstraintType type;
            Object value = null;
            ForeignKeyClause foreignKey = null;
            Expression checkExpression = null;

            if (constraintCtx.NULL() != null) {
                type = constraintCtx.NOT() != null ? ConstraintType.NOT_NULL : ConstraintType.NULL;
            }
            else if (constraintCtx.PRIMARY() != null) {
                type = ConstraintType.PRIMARY_KEY;
            }
            else if (constraintCtx.UNIQUE() != null) {
                type = ConstraintType.UNIQUE;
            }
            else if (constraintCtx.DEFAULT() != null) {
                type = ConstraintType.DEFAULT;
                value = processDefaultValue(constraintCtx.defaultValue());
            }
            else if (constraintCtx.foreignKeyClause() != null) {
                type = ConstraintType.FOREIGN_KEY;
                foreignKey = processForeignKeyClause(constraintCtx.foreignKeyClause());
            }
            else if (constraintCtx.checkConstraint() != null) {
                type = ConstraintType.CHECK;
                checkExpression = processExpression(constraintCtx.checkConstraint().expression());
            }
            else {
                continue;  // Unknown constraint type
            }

            constraints.add(new ColumnConstraint(constraintName, type, value, foreignKey, checkExpression));
        }

        return new ColumnElement(columnName, dataType, constraints.toArray(new ColumnConstraint[0]));
    }

    private TableConstraint processTableConstraint(SqlBaseParser.TableConstraintContext ctx)
    {
        String constraintName = ctx.constraintName() != null ? ctx.constraintName().getText() : null;
        ConstraintType type;
        String[] columns = null;
        ForeignKeyClause foreignKey = null;
        Expression checkExpression = null;

        if (ctx.primaryKeyConstraint() != null) {
            type = ConstraintType.PRIMARY_KEY;
            columns = ctx.primaryKeyConstraint().columnName().stream()
                    .map(RuleContext::getText)
                    .toArray(String[]::new);
        }
        else if (ctx.uniqueConstraint() != null) {
            type = ConstraintType.UNIQUE;
            columns = ctx.uniqueConstraint().columnName().stream()
                    .map(RuleContext::getText)
                    .toArray(String[]::new);
        }
        else if (ctx.foreignKeyConstraint() != null) {
            type = ConstraintType.FOREIGN_KEY;
            columns = ctx.foreignKeyConstraint().columnName().stream()
                    .map(RuleContext::getText)
                    .toArray(String[]::new);
            foreignKey = processForeignKeyClause(ctx.foreignKeyConstraint().foreignKeyClause());
        }
        else if (ctx.checkConstraint() != null) {
            type = ConstraintType.CHECK;
            checkExpression = processExpression(ctx.checkConstraint().expression());
        }
        else {
            throw new IllegalStateException("Unknown constraint type");
        }

        return new TableConstraint(constraintName, type, columns, foreignKey, checkExpression);
    }

    private ForeignKeyClause processForeignKeyClause(SqlBaseParser.ForeignKeyClauseContext ctx)
    {
        String referencedTable = ctx.tableName().getText();

        String[] referencedColumns = null;
        if (ctx.columnName() != null && !ctx.columnName().isEmpty()) {
            referencedColumns = ctx.columnName().stream()
                    .map(RuleContext::getText)
                    .toArray(String[]::new);
        }

        ReferenceOption onDelete = null;
        ReferenceOption onUpdate = null;

        if (ctx.DELETE() != null) {
            onDelete = getReferenceOption(ctx.referenceOption(0));
        }
        if (ctx.UPDATE() != null) {
            onUpdate = getReferenceOption(ctx.referenceOption(1));
        }

        return new ForeignKeyClause(referencedTable, referencedColumns, onDelete, onUpdate);
    }

    private DataType processDataType(SqlBaseParser.DataTypeContext ctx)
    {
        String baseType = normalizeDataType(ctx.baseDataType().getText());

        Integer[] parameters = null;
        if (ctx.INTEGER_VALUE() != null && !ctx.INTEGER_VALUE().isEmpty()) {
            parameters = ctx.INTEGER_VALUE().stream()
                    .map(node -> Integer.parseInt(node.getText()))
                    .toArray(Integer[]::new);
        }

        return new DataType(baseType, parameters);
    }

    private String normalizeDataType(String baseType)
    {
        // Normalize case and handle type aliases
        switch (baseType.toUpperCase()) {
            case "INT":
            case "INTEGER":
                return "INTEGER";
            case "BOOL":
            case "BOOLEAN":
                return "BOOLEAN";
            case "DEC":
            case "DECIMAL":
            case "NUMERIC":
                return "DECIMAL";
            case "CHAR":
            case "CHARACTER":
                return "CHARACTER";
            default:
                return baseType.toUpperCase();
        }
    }

    private ReferenceOption getReferenceOption(SqlBaseParser.ReferenceOptionContext ctx)
    {
        if (ctx.RESTRICT() != null) {
            return ReferenceOption.RESTRICT;
        }
        if (ctx.CASCADE() != null) {
            return ReferenceOption.CASCADE;
        }
        if (ctx.NULL() != null) {
            return ReferenceOption.SET_NULL;
        }
        if (ctx.NO() != null) {
            return ReferenceOption.NO_ACTION;
        }
        if (ctx.DEFAULT() != null) {
            return ReferenceOption.SET_DEFAULT;
        }
        return ReferenceOption.NO_ACTION; // Default behavior
    }

    private Object processDefaultValue(SqlBaseParser.DefaultValueContext ctx)
    {
        if (ctx.literal() != null) {
            return processLiteral(ctx.literal());
        }
        else if (ctx.expression() != null) {
            return processExpression(ctx.expression());
        }
        return null;
    }

    private Object processLiteral(SqlBaseParser.LiteralContext ctx)
    {
        if (ctx.STRING() != null) {
            return unquoteString(ctx.STRING().getText());
        }
        else if (ctx.INTEGER_VALUE() != null) {
            return Long.parseLong(ctx.INTEGER_VALUE().getText());
        }
        else if (ctx.DECIMAL_VALUE() != null) {
            return Double.parseDouble(ctx.DECIMAL_VALUE().getText());
        }
        else if (ctx.TRUE() != null) {
            return true;
        }
        else if (ctx.FALSE() != null) {
            return false;
        }
        else if (ctx.NULL() != null) {
            return null;
        }
        return null;
    }

    private String unquoteString(String str)
    {
        if (str == null || str.length() < 2) {
            return str;
        }
        // Remove surrounding quotes (either single or double quotes)
        return str.substring(1, str.length() - 1);
    }

    @Override
    public SQLStatement visitAlterStatement(SqlBaseParser.AlterStatementContext ctx)
    {
        // TODO: Implement alter statement parsing
        return null;
    }

    @Override
    public SQLStatement visitDropStatement(SqlBaseParser.DropStatementContext ctx)
    {
        if (ctx.dropDatabaseStatement() != null) {
            return visitDropDatabaseStatement(ctx.dropDatabaseStatement());
        }

        if (ctx.dropTableStatement() != null) {
            return visitDropTableStatement(ctx.dropTableStatement());
        }
        return null;
    }

    @Override
    public SQLStatement visitDropDatabaseStatement(SqlBaseParser.DropDatabaseStatementContext ctx)
    {
        boolean ifNotExists = ctx.EXISTS() != null;
        return new DropDatabaseStatement(ctx.databaseName().getText(), ifNotExists);
    }

    @Override
    public SQLStatement visitDropTableStatement(SqlBaseParser.DropTableStatementContext ctx)
    {
        boolean ifNotExists = ctx.EXISTS() != null;
        List<String> tableNames = ctx.tableName().stream()
                .map(RuleContext::getText)
                .collect(Collectors.toList());
        return new DropTableStatement(tableNames, ifNotExists);
    }

    @Override
    public SQLStatement visitUseStatement(SqlBaseParser.UseStatementContext ctx)
    {
        return new UseDatabaseStatement(ctx.databaseName().getText());
    }

    @Override
    public SQLStatement visitShowStatement(SqlBaseParser.ShowStatementContext ctx)
    {
        ShowProcessor processor = new ShowProcessor();
        return processor.process(ctx);
    }

    @Override
    public SQLStatement visitQuerySpecification(SqlBaseParser.QuerySpecificationContext ctx)
    {
        SelectStatement statement = new SelectStatement();
        statement.setSelectElements(processSelectElements(ctx.selectElements()));

        if (ctx.fromClause() != null) {
            statement.setFromSources(processFromClause(ctx.fromClause()));
        }

        if (ctx.whereClause() != null) {
            statement.setWhereClause(processExpression(ctx.whereClause().expression()));
        }

        if (ctx.groupByClause() != null) {
            statement.setGroupByElements(visitGroupByElements(ctx.groupByClause()));
        }

        if (ctx.havingClause() != null) {
            statement.setHavingClause(processExpression(ctx.havingClause().expression()));
        }

        return statement;
    }

    private List<SelectElement> processSelectElements(SqlBaseParser.SelectElementsContext ctx)
    {
        List<SelectElement> elements = new ArrayList<>();

        for (SqlBaseParser.SelectElementContext elementCtx : ctx.selectElement()) {
            SelectElement element = new SelectElement();

            if (elementCtx.columnName() != null) {
                // 直接指定的列名
                // Directly specified column names
                element.setColumn(elementCtx.columnName().getText());
            }
            // 处理表达式
            // Handle expression
            if (elementCtx.expression() != null) {
                Expression expr = processExpression(elementCtx.expression());
                element.setExpression(expr);

                // 处理函数调用的情况
                // Handle function call
                if (expr.getType() == Expression.ExpressionType.FUNCTION) {
                    // 尝试从函数的参数中获取列名
                    // Try to get column name from function parameters
                    if (expr.getChildren() != null && !expr.getChildren().isEmpty()) {
                        Expression columnExpr = expr.getChildren().get(0);
                        if (columnExpr.getType() == Expression.ExpressionType.COLUMN_REFERENCE) {
                            element.setColumn(columnExpr.getValue().toString());
                        }
                    }
                }
            }
            // 处理别名
            // Handle alias
            if (elementCtx.alias() != null) {
                element.setAlias(elementCtx.alias().getText());
            }

            elements.add(element);
        }

        return elements;
    }

    private List<TableElement> processFromClause(SqlBaseParser.FromClauseContext ctx)
    {
        List<TableElement> tables = new ArrayList<>();

        for (SqlBaseParser.TableSourceContext sourceCtx : ctx.tableSource()) {
            TableElement table = new TableElement();

            // 普通获取主表信息
            // Get the primary table information
            if (sourceCtx.tablePrimary() != null) {
                SqlBaseParser.TablePrimaryContext primaryCtx = sourceCtx.tablePrimary();

                // 处理子查询
                // Handle subquery
                if (primaryCtx.selectStatement() != null) {
                    // 处理子查询的别名
                    // Handle the alias of the subquery
                    if (primaryCtx.alias() != null) {
                        table.setAlias(primaryCtx.alias().getText());
                    }
                    // 可以选择存储子查询的SelectStatement
                    SelectStatement subquery = (SelectStatement) visit(primaryCtx.selectStatement());
                    table.setSubquery(subquery);
                }
                // 处理普通表
                // Handle normal tables
                else if (primaryCtx.tableName() != null) {
                    table.setTableName(primaryCtx.tableName().getText());
                    if (primaryCtx.alias() != null) {
                        table.setAlias(primaryCtx.alias().getText());
                    }
                }
            }

            if (sourceCtx.joinedTable() != null) {
                List<JoinClause> joins = new ArrayList<>();

                // 从 joinedTable 的 tablePrimary 中获取主表信息
                // Get the primary table information from the tablePrimary of joinedTable
                SqlBaseParser.TablePrimaryContext primaryCtx = sourceCtx.joinedTable().tablePrimary();
                if (primaryCtx != null && primaryCtx.tableName() != null) {
                    table.setTableName(primaryCtx.tableName().getText());
                    if (primaryCtx.alias() != null) {
                        table.setAlias(primaryCtx.alias().getText());
                    }
                }

                // 处理 joins
                // Handle joins
                for (SqlBaseParser.JoinClauseContext joinCtx : sourceCtx.joinedTable().joinClause()) {
                    JoinClause join = processJoinClause(joinCtx);
                    joins.add(join);
                }
                table.setJoins(joins);
            }

            tables.add(table);
        }

        return tables;
    }

    private JoinClause processJoinClause(SqlBaseParser.JoinClauseContext ctx)
    {
        JoinClause join = new JoinClause();

        if (ctx.joinTypeClause() != null) {
            if (ctx.joinTypeClause().INNER() != null) {
                join.setJoinType(JoinClause.JoinType.INNER);
            }
            else if (ctx.joinTypeClause().LEFT() != null) {
                join.setJoinType(JoinClause.JoinType.LEFT);
            }
            else if (ctx.joinTypeClause().RIGHT() != null) {
                join.setJoinType(JoinClause.JoinType.RIGHT);
            }
            else if (ctx.joinTypeClause().FULL() != null) {
                join.setJoinType(JoinClause.JoinType.FULL);
            }
        }

        TableElement rightTable = new TableElement();
        rightTable.setTableName(ctx.tablePrimary().tableName().getText());
        if (ctx.tablePrimary().alias() != null) {
            rightTable.setAlias(ctx.tablePrimary().alias().getText());
        }
        join.setRightTable(rightTable);

        if (ctx.joinCondition() != null) {
            if (ctx.joinCondition().ON() != null) {
                join.setCondition(processExpression(ctx.joinCondition().expression()));
            }
        }

        return join;
    }

    private Expression processExpression(SqlBaseParser.ExpressionContext ctx)
    {
        ExpressionProcessor processor = new ExpressionProcessor();
        return processor.visit(ctx);
    }

    private List<Expression> visitGroupByElements(SqlBaseParser.GroupByClauseContext ctx)
    {
        List<Expression> groupByElements = new ArrayList<>();

        for (SqlBaseParser.GroupByElementContext elementCtx : ctx.groupByElement()) {
            groupByElements.add(processExpression(elementCtx.expression()));
        }

        return groupByElements;
    }

    private List<OrderByElement> visitOrderByElements(SqlBaseParser.OrderByClauseContext ctx)
    {
        List<OrderByElement> orderByElements = new ArrayList<>();

        for (SqlBaseParser.OrderByElementContext elementCtx : ctx.orderByElement()) {
            OrderByElement element = new OrderByElement();
            element.setExpression(processExpression(elementCtx.expression()));
            element.setAscending(elementCtx.DESC() == null);
            orderByElements.add(element);
        }

        return orderByElements;
    }

    private LimitClause processLimitClause(SqlBaseParser.LimitClauseContext ctx)
    {
        LimitClause limit = new LimitClause();

        if (ctx.INTEGER_VALUE().size() > 1) {
            limit.setOffset(Long.parseLong(ctx.INTEGER_VALUE(0).getText()));
            limit.setLimit(Long.parseLong(ctx.INTEGER_VALUE(1).getText()));
        }
        else {
            limit.setLimit(Long.parseLong(ctx.INTEGER_VALUE(0).getText()));
            if (ctx.OFFSET() != null) {
                limit.setOffset(Long.parseLong(ctx.INTEGER_VALUE(1).getText()));
            }
        }

        return limit;
    }
}
