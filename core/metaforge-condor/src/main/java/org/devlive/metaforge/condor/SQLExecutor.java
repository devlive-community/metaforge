package org.devlive.metaforge.condor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.condor.condition.AndCondition;
import org.devlive.metaforge.condor.condition.Condition;
import org.devlive.metaforge.condor.condition.OrCondition;
import org.devlive.metaforge.condor.condition.SimpleCondition;
import org.devlive.metaforge.condor.manager.DatabaseManager;
import org.devlive.metaforge.condor.manager.TableManager;
import org.devlive.metaforge.condor.metadata.ColumnDefinition;
import org.devlive.metaforge.condor.metadata.DatabaseDefinition;
import org.devlive.metaforge.condor.metadata.RowDefinition;
import org.devlive.metaforge.condor.metadata.TableDefinition;
import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.node.ColumnConstraint;
import org.devlive.metaforge.sql.node.ConstraintType;
import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.node.TableConstraint;
import org.devlive.metaforge.sql.node.element.ColumnElement;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.node.element.TableElement;
import org.devlive.metaforge.sql.statement.CreateDatabaseStatement;
import org.devlive.metaforge.sql.statement.CreateTableStatement;
import org.devlive.metaforge.sql.statement.DeleteStatement;
import org.devlive.metaforge.sql.statement.DropDatabaseStatement;
import org.devlive.metaforge.sql.statement.DropTableStatement;
import org.devlive.metaforge.sql.statement.InsertStatement;
import org.devlive.metaforge.sql.statement.SQLStatement;
import org.devlive.metaforge.sql.statement.SelectStatement;
import org.devlive.metaforge.sql.statement.ShowStatement;
import org.devlive.metaforge.sql.statement.UpdateStatement;
import org.devlive.metaforge.sql.statement.UseDatabaseStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressFBWarnings(value = {"REC_CATCH_EXCEPTION"})
public class SQLExecutor
{
    private final DatabaseManager databaseManager;
    private TableManager tableManager;

    public SQLExecutor(DatabaseManager databaseManager)
    {
        this.databaseManager = databaseManager;
    }

    public <T> SQLResult<T> execute(String sql)
    {
        try {
            SQLStatement statement = SQLParser.parse(sql);

            if (statement instanceof CreateDatabaseStatement) {
                return (SQLResult<T>) executeCreateDatabase((CreateDatabaseStatement) statement);
            }

            if (statement instanceof DropDatabaseStatement) {
                return (SQLResult<T>) executeDropDatabase((DropDatabaseStatement) statement);
            }

            if (statement instanceof UseDatabaseStatement) {
                return (SQLResult<T>) executeUseDatabase((UseDatabaseStatement) statement);
            }

            if (statement instanceof ShowStatement) {
                return (SQLResult<T>) executeShow((ShowStatement) statement);
            }

            if (statement instanceof CreateTableStatement) {
                ensureCurrentTableManager();
                return (SQLResult<T>) executeCreateTable((CreateTableStatement) statement);
            }

            if (statement instanceof DropTableStatement) {
                ensureCurrentTableManager();
                return (SQLResult<T>) executeDropTable((DropTableStatement) statement);
            }

            if (statement instanceof InsertStatement) {
                ensureCurrentTableManager();
                return (SQLResult<T>) executeInsert((InsertStatement) statement);
            }

            if (statement instanceof SelectStatement) {
                ensureCurrentTableManager();
                return (SQLResult<T>) executeSelect((SelectStatement) statement);
            }

            if (statement instanceof UpdateStatement) {
                ensureCurrentTableManager();
                return (SQLResult<T>) executeUpdate((UpdateStatement) statement);
            }

            if (statement instanceof DeleteStatement) {
                ensureCurrentTableManager();
                return (SQLResult<T>) executeDelete((DeleteStatement) statement);
            }

            return new SQLResult<>(false, String.format("Unsupported SQL statement: %s", statement));
        }
        catch (Exception e) {
            return new SQLResult<>(false, e.getMessage());
        }
    }

    private void ensureCurrentTableManager()
            throws DatabaseException
    {
        DatabaseDefinition currentDatabase = databaseManager.getCurrentDatabase();
        if (tableManager == null && currentDatabase != null) {
            tableManager = currentDatabase.getTableManager();
        }
    }

    private SQLResult<Void> executeCreateDatabase(CreateDatabaseStatement statement)
    {
        try {
            String databaseName = statement.getDatabaseName();

            if (statement.isIfNotExists() && databaseManager.databaseExists(databaseName)) {
                return new SQLResult<>(true, "Database already exists");
            }

            databaseManager.createDatabase(databaseName);
            return new SQLResult<>(true, "Database created successfully");
        }
        catch (DatabaseException e) {
            return new SQLResult<>(false, "Failed to create database: " + e.getMessage());
        }
    }

    private SQLResult<Void> executeDropDatabase(DropDatabaseStatement statement)
    {
        try {
            if (statement.isIfNotExists() && !databaseManager.databaseExists(statement.getDatabaseName())) {
                return new SQLResult<>(true, "Database does not exist");
            }

            databaseManager.dropDatabase(statement.getDatabaseName());
            return new SQLResult<>(true, "Database dropped successfully");
        }
        catch (DatabaseException e) {
            return new SQLResult<>(false, "Failed to drop database: " + e.getMessage());
        }
    }

    private SQLResult<Void> executeUseDatabase(UseDatabaseStatement statement)
    {
        try {
            String databaseName = statement.getDatabaseName();
            databaseManager.useDatabase(databaseName);
            tableManager = null;
            return new SQLResult<>(true, "Database changed");
        }
        catch (DatabaseException e) {
            return new SQLResult<>(false, "Failed to use database: " + e.getMessage());
        }
    }

    private SQLResult<List<RowDefinition>> executeShow(ShowStatement statement)
    {
        try {
            List<RowDefinition> rows = new ArrayList<>();

            switch (statement.getShowType()) {
                case DATABASES:
                    for (String dbName : databaseManager.listDatabases()) {
                        RowDefinition row = new RowDefinition();
                        row.setValue("Database", dbName);
                        rows.add(row);
                    }
                    break;
                case TABLES:
                    ensureCurrentTableManager();
                    for (String tableName : tableManager.listTables()) {
                        RowDefinition row = new RowDefinition();
                        row.setValue("Table", tableName);
                        rows.add(row);
                    }
                    break;
                case COLUMNS:
                    ensureCurrentTableManager();
                    String tableName = statement.getTableName();
                    TableDefinition metadata = tableManager.getTableMetadata(tableName);
                    for (ColumnDefinition col : metadata.getColumns()) {
                        RowDefinition row = new RowDefinition();
                        row.setValue("Field", col.getName());
                        row.setValue("Type", col.getType().name());
                        row.setValue("Null", col.isNullable() ? "YES" : "NO");
                        row.setValue("Key", col.isPrimaryKey() ? "PRI" : "");
                        rows.add(row);
                    }
                    break;
                default:
                    return new SQLResult<>(false, "Unsupported SHOW type: " + statement.getShowType());
            }

            return new SQLResult<>(true, String.format("Show %d rows", rows.size()), rows);
        }
        catch (Exception e) {
            return new SQLResult<>(false, "Failed to execute SHOW: " + e.getMessage());
        }
    }

    private SQLResult<Void> executeCreateTable(CreateTableStatement statement)
    {
        try {
            if (statement.isIfNotExists() && tableManager.tableExists(statement.getTableName())) {
                return new SQLResult<>(true, "Table already exists");
            }

            List<ColumnDefinition> columns = convertToColumns(statement.getColumns());

            TableDefinition metadata = new TableDefinition(statement.getTableName(), columns);
            tableManager.createTable(metadata);
            return new SQLResult<>(true, "Table created successfully");
        }
        catch (Exception e) {
            return new SQLResult<>(false, "Failed to create table: " + e.getMessage());
        }
    }

    private SQLResult<Void> executeDropTable(DropTableStatement statement)
    {
        try {
            String tableName = statement.getTableNames().get(0);
            if (statement.isIfExists() && !tableManager.tableExists(tableName)) {
                return new SQLResult<>(true, "Table does not exist");
            }

            tableManager.dropTable(tableName);
            return new SQLResult<>(true, "Table dropped successfully");
        }
        catch (Exception e) {
            return new SQLResult<>(false, "Failed to drop table: " + e.getMessage());
        }
    }

    private SQLResult<Integer> executeInsert(InsertStatement statement)
    {
        try {
            List<List<Object>> simpleValues = statement.getSimpleValues()
                    .stream()
                    .map(row -> row.stream()
                            .map(SQLExecutor::unquoteLiteral)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());
            if (simpleValues.size() == 1) {
                tableManager.insert(
                        statement.getTableName(),
                        statement.getColumns(),
                        simpleValues.get(0)
                );
            }
            else {
                tableManager.batchInsert(
                        statement.getTableName(),
                        statement.getColumns(),
                        simpleValues
                );
            }

            return new SQLResult<>(true, String.format("Inserted %d rows", statement.getSimpleValues().size()));
        }
        catch (Exception e) {
            return new SQLResult<>(false, "Failed to insert rows: " + e.getMessage());
        }
    }

    private SQLResult<List<RowDefinition>> executeSelect(SelectStatement statement)
    {
        try {
            Condition whereCondition = null;
            if (statement.getWhereClause() != null) {
                whereCondition = convertExpressionToCondition(statement.getWhereClause());
            }

            List<String> columnNames = statement.getSelectElements().stream()
                    .map(SelectElement::getColumn)
                    .collect(Collectors.toList());

            boolean isSelectAll = columnNames.stream().anyMatch("*"::equals);

            List<RowDefinition> rows = tableManager.select(
                    statement.getFromSources().get(0).getTableName(),
                    isSelectAll ? null : columnNames,
                    whereCondition
            );
            return new SQLResult<>(
                    true,
                    String.format("Selected %d rows", rows.size()),
                    rows
            );
        }
        catch (Exception e) {
            return new SQLResult<>(false, "Failed to select rows: " + e.getMessage());
        }
    }

    private SQLResult<Integer> executeUpdate(UpdateStatement statement)
    {
        try {
            Condition whereCondition = null;
            if (statement.getWhereClause() != null) {
                whereCondition = convertExpressionToCondition(statement.getWhereClause());
            }

            Map<String, Object> setValues = new HashMap<>();
            for (Map.Entry<String, Expression> entry : statement.getSetValues().entrySet()) {
                setValues.put(entry.getKey(), unquoteLiteral(entry.getValue().getValue()));
            }

            int updatedCount = tableManager.update(
                    statement.getTableName(),
                    setValues,
                    whereCondition
            );
            return new SQLResult<>(true, String.format("Updated %d rows", updatedCount), updatedCount);
        }
        catch (Exception e) {
            return new SQLResult<>(false, "Failed to update rows: " + e.getMessage());
        }
    }

    private SQLResult<Integer> executeDelete(DeleteStatement statement)
    {
        try {
            Condition whereCondition = null;
            if (statement.getWhereClause() != null) {
                whereCondition = convertExpressionToCondition(statement.getWhereClause());
            }

            int deletedCount = tableManager.delete(
                    statement.getTableName(),
                    whereCondition
            );
            return new SQLResult<>(true, String.format("Deleted %d rows", deletedCount), deletedCount);
        }
        catch (Exception e) {
            return new SQLResult<>(false, "Failed to delete rows: " + e.getMessage());
        }
    }

    private Condition convertExpressionToCondition(Expression expression)
    {
        if (expression == null) {
            return null;
        }

        if (expression.getType() == Expression.ExpressionType.BINARY_OP) {
            String operator = (String) expression.getValue();
            List<Expression> children = expression.getChildren();

            if ("AND".equalsIgnoreCase(operator)) {
                Condition left = convertExpressionToCondition(children.get(0));
                Condition right = convertExpressionToCondition(children.get(1));
                return new AndCondition(left, right);
            }

            if ("OR".equalsIgnoreCase(operator)) {
                Condition left = convertExpressionToCondition(children.get(0));
                Condition right = convertExpressionToCondition(children.get(1));
                return new OrCondition(left, right);
            }

            Expression leftExpr = children.get(0);
            Expression rightExpr = children.get(1);

            String columnName = (String) leftExpr.getValue();
            Object value = unquoteLiteral(rightExpr.getValue());

            ComparisonOperator compOp = convertOperator(operator);
            return new SimpleCondition(columnName, value, compOp);
        }

        return null;
    }

    /**
     * 剥离 SQL 字符串字面量包裹的单引号（解析器保留原始文本），
     * 保证存储值与 WHERE / SET 字面量比较时的一致性
     */
    private static Object unquoteLiteral(Object value)
    {
        if (value instanceof String) {
            String text = (String) value;
            if (text.length() >= 2 && text.startsWith("'") && text.endsWith("'")) {
                return text.substring(1, text.length() - 1).replace("''", "'");
            }
        }
        return value;
    }

    private ComparisonOperator convertOperator(String operator)
    {
        switch (operator) {
            case "=":
                return ComparisonOperator.EQUALS;
            case "!=":
            case "<>":
                return ComparisonOperator.NOT_EQUALS;
            case ">":
                return ComparisonOperator.GREATER_THAN;
            case ">=":
                return ComparisonOperator.GREATER_THAN_OR_EQUALS;
            case "<":
                return ComparisonOperator.LESS_THAN;
            case "<=":
                return ComparisonOperator.LESS_THAN_OR_EQUALS;
            default:
                return ComparisonOperator.EQUALS;
        }
    }

    private List<ColumnDefinition> convertToColumns(List<TableElement> elements)
    {
        List<ColumnDefinition> columns = new ArrayList<>();

        // First pass: collect all columns and primary key constraints
        Set<String> primaryKeyColumns = new HashSet<>();
        for (TableElement element : elements) {
            if (element instanceof TableConstraint) {
                TableConstraint constraint = (TableConstraint) element;
                if (constraint.getType() == ConstraintType.PRIMARY_KEY && constraint.getColumns() != null) {
                    primaryKeyColumns.addAll(Arrays.asList(constraint.getColumns()));
                }
            }
        }

        // Second pass: create column definitions
        for (TableElement element : elements) {
            if (element instanceof ColumnElement) {
                ColumnElement col = (ColumnElement) element;
                boolean isPrimaryKey = primaryKeyColumns.contains(col.getColumnName());
                boolean isNullable = true;

                // Check column constraints
                for (ColumnConstraint constraint : col.getConstraints()) {
                    if (constraint.getType() == ConstraintType.PRIMARY_KEY) {
                        isPrimaryKey = true;
                    }
                    else if (constraint.getType() == ConstraintType.NOT_NULL) {
                        isNullable = false;
                    }
                }

                DataType type = convertDataType(col.getDataType());
                columns.add(new ColumnDefinition(
                        col.getColumnName(),
                        type,
                        isPrimaryKey,
                        isNullable));
            }
        }

        return columns;
    }

    private DataType convertDataType(org.devlive.metaforge.sql.node.DataType sourceType)
    {
        return DataType.valueOf(sourceType.getBaseType());
    }
}
