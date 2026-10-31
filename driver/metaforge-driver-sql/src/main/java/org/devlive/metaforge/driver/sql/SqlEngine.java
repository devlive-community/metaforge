package org.devlive.metaforge.driver.sql;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.sql.SQLParseException;
import org.devlive.metaforge.sql.SQLParser;
import org.devlive.metaforge.sql.node.ColumnConstraint;
import org.devlive.metaforge.sql.node.ConstraintType;
import org.devlive.metaforge.sql.node.Expression;
import org.devlive.metaforge.sql.node.TableConstraint;
import org.devlive.metaforge.sql.node.element.ColumnElement;
import org.devlive.metaforge.sql.node.element.OrderByElement;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.node.element.TableElement;
import org.devlive.metaforge.sql.statement.AlterTableStatement;
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

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Runs parsed SQL against a {@link SqlSource}: SHOW, USE, CREATE / DROP DATABASE, CREATE / DROP
 * / ALTER TABLE, SELECT with WHERE, ORDER BY and LIMIT, INSERT, UPDATE and DELETE. A script runs
 * statement by statement and reports the last query, or the rows all statements changed.
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "JDBC objects share their connection, statement and source")
public final class SqlEngine
{
    private final SqlSource source;
    private String database;

    public SqlEngine(SqlSource source, String database)
    {
        this.source = source;
        this.database = database == null || database.isEmpty() ? null : database;
    }

    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }

    public Rows execute(String sql)
            throws SQLException
    {
        List<SQLStatement> statements;
        try {
            statements = SQLParser.parseMultiple(sql);
        }
        catch (SQLParseException error) {
            throw new SQLSyntaxErrorException(error.getMessage(), error);
        }
        if (statements.isEmpty()) {
            throw new SQLSyntaxErrorException("Unsupported statement: " + sql.trim());
        }
        Rows result = null;
        int changed = 0;
        for (SQLStatement statement : statements) {
            result = run(statement);
            if (!result.isQuery()) {
                changed += result.getUpdateCount();
            }
        }
        return result.isQuery() ? result : Rows.updated(changed);
    }

    private Rows run(SQLStatement statement)
            throws SQLException
    {
        if (statement instanceof SelectStatement) {
            return select((SelectStatement) statement);
        }
        if (statement instanceof ShowStatement) {
            return show((ShowStatement) statement);
        }
        if (statement instanceof UseDatabaseStatement) {
            String name = Names.unquote(((UseDatabaseStatement) statement).getDatabaseName());
            if (!source.databases().contains(name)) {
                throw new SQLException("Unknown database " + name);
            }
            database = name;
            return Rows.updated(0);
        }
        if (statement instanceof CreateDatabaseStatement) {
            CreateDatabaseStatement create = (CreateDatabaseStatement) statement;
            String name = Names.unquote(create.getDatabaseName());
            if (source.databases().contains(name)) {
                if (create.isIfNotExists()) {
                    return Rows.updated(0);
                }
                throw new SQLException("Database " + name + " already exists");
            }
            source.createDatabase(name);
            return Rows.updated(1);
        }
        if (statement instanceof DropDatabaseStatement) {
            DropDatabaseStatement drop = (DropDatabaseStatement) statement;
            String name = Names.unquote(drop.getDatabaseName());
            if (!source.databases().contains(name)) {
                if (drop.isIfNotExists()) {
                    return Rows.updated(0);
                }
                throw new SQLException("Unknown database " + name);
            }
            source.dropDatabase(name);
            if (name.equals(database)) {
                database = null;
            }
            return Rows.updated(1);
        }
        if (statement instanceof CreateTableStatement) {
            return createTable((CreateTableStatement) statement);
        }
        if (statement instanceof DropTableStatement) {
            DropTableStatement drop = (DropTableStatement) statement;
            int count = 0;
            for (String name : drop.getTableNames()) {
                String[] table = Names.table(name, database);
                if (!source.tables(table[0]).contains(table[1])) {
                    if (drop.isIfExists()) {
                        continue;
                    }
                    throw new SQLException("Unknown table " + table[0] + "." + table[1]);
                }
                source.dropTable(table[0], table[1]);
                count++;
            }
            return Rows.updated(count);
        }
        if (statement instanceof AlterTableStatement) {
            return alter((AlterTableStatement) statement);
        }
        if (statement instanceof InsertStatement) {
            return insert((InsertStatement) statement);
        }
        if (statement instanceof UpdateStatement) {
            UpdateStatement update = (UpdateStatement) statement;
            String[] table = existing(update.getTableName());
            Map<String, Object> values = new LinkedHashMap<>();
            List<String> names = columnNames(table);
            for (Map.Entry<String, Expression> entry : update.getSetValues().entrySet()) {
                String column = known(names, Predicates.column(entry.getKey()));
                if (entry.getValue().getType() != Expression.ExpressionType.LITERAL) {
                    throw new SQLException("Only literal values can be set, not " + entry.getValue().getValue());
                }
                values.put(column, Names.literal(entry.getValue().getValue()));
            }
            return Rows.updated(source.update(table[0], table[1], values, Predicates.of(update.getWhereClause())));
        }
        if (statement instanceof DeleteStatement) {
            DeleteStatement delete = (DeleteStatement) statement;
            String[] table = existing(delete.getTableName());
            return Rows.updated(source.delete(table[0], table[1], Predicates.of(delete.getWhereClause())));
        }
        throw new SQLSyntaxErrorException("Unsupported statement: " + statement.getClass().getSimpleName());
    }

    private Rows select(SelectStatement select)
            throws SQLException
    {
        List<TableElement> from = select.getFromSources();
        if (from == null || from.isEmpty()) {
            return constants(select.getSelectElements());
        }
        if (from.size() > 1 || (from.get(0).getJoins() != null && !from.get(0).getJoins().isEmpty()) || from.get(0).getSubquery() != null) {
            throw new SQLFeatureNotSupportedException("Joins and subqueries are not supported");
        }
        if ((select.getGroupByElements() != null && !select.getGroupByElements().isEmpty()) || select.getHavingClause() != null) {
            throw new SQLFeatureNotSupportedException("GROUP BY is not supported");
        }
        String[] table = existing(from.get(0).getTableName());
        List<SourceColumn> columns = source.columns(table[0], table[1]);
        List<String> names = columns.stream().map(SourceColumn::getName).collect(Collectors.toList());

        List<String> labels = new ArrayList<>();
        List<String> types = new ArrayList<>();
        List<Integer> picked = new ArrayList<>();
        for (SelectElement element : select.getSelectElements()) {
            if (element.getExpression() != null) {
                throw new SQLFeatureNotSupportedException("Only columns can be selected from a table, not " + element.getExpression().getValue());
            }
            if (element.getColumn() == null || element.getColumn().equals("*") || element.getColumn().endsWith(".*")) {
                for (int i = 0; i < columns.size(); i++) {
                    labels.add(names.get(i));
                    types.add(columns.get(i).getType());
                    picked.add(i);
                }
                continue;
            }
            String name = known(names, Predicates.column(element.getColumn()));
            int index = names.indexOf(name);
            labels.add(element.getAlias() == null ? name : Names.unquote(element.getAlias()));
            types.add(columns.get(index).getType());
            picked.add(index);
        }

        Predicate<Map<String, Object>> filter = Predicates.of(select.getWhereClause());
        List<OrderByElement> order = select.getOrderByElements() == null ? List.of() : select.getOrderByElements();
        long offset = select.getLimitClause() == null ? 0 : select.getLimitClause().getOffset();
        long limit = select.getLimitClause() == null ? -1 : select.getLimitClause().getLimit();
        boolean streaming = select.getWhereClause() == null && order.isEmpty();
        List<List<Object>> rows = source.rows(table[0], table[1], streaming && limit >= 0 ? offset + limit : -1);
        List<Map<String, Object>> matched = new ArrayList<>();
        for (List<Object> row : rows) {
            Map<String, Object> named = named(names, row);
            if (filter.test(named)) {
                matched.add(named);
            }
        }
        for (int i = order.size() - 1; i >= 0; i--) {
            OrderByElement element = order.get(i);
            if (element.getExpression() == null || element.getExpression().getType() != Expression.ExpressionType.COLUMN_REFERENCE) {
                throw new SQLFeatureNotSupportedException("Only columns can order rows");
            }
            String name = known(names, Predicates.column(String.valueOf(element.getExpression().getValue())));
            Comparator<Map<String, Object>> comparator = (a, b) -> Predicates.compare(a.get(name), b.get(name));
            matched.sort(element.isAscending() ? comparator : comparator.reversed());
        }
        List<List<Object>> values = new ArrayList<>();
        for (int i = (int) Math.min(offset, matched.size()); i < matched.size() && (limit < 0 || values.size() < limit); i++) {
            Map<String, Object> row = matched.get(i);
            List<Object> projected = new ArrayList<>();
            for (int index : picked) {
                projected.add(row.get(names.get(index)));
            }
            values.add(projected);
        }
        return Rows.of(labels, types, values);
    }

    /** SELECT without FROM: literals and version(), as connection checks send them. */
    private Rows constants(List<SelectElement> elements)
            throws SQLException
    {
        List<String> labels = new ArrayList<>();
        List<String> types = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        for (SelectElement element : elements) {
            Expression expression = element.getExpression();
            Object value;
            String label;
            if (expression != null && expression.getType() == Expression.ExpressionType.LITERAL) {
                value = Names.literal(expression.getValue());
                label = String.valueOf(expression.getValue());
            }
            else if (expression != null && expression.getType() == Expression.ExpressionType.FUNCTION && "VERSION".equalsIgnoreCase(String.valueOf(expression.getValue()))) {
                value = source.productVersion();
                label = "version()";
            }
            else {
                throw new SQLFeatureNotSupportedException("Only literals and version() can be selected without a table");
            }
            labels.add(element.getAlias() == null ? label : Names.unquote(element.getAlias()));
            types.add(value instanceof Integer ? "INTEGER" : value instanceof Long ? "BIGINT" : value instanceof Double ? "DOUBLE" : "VARCHAR");
            row.add(value);
        }
        return Rows.of(labels, types, List.of(row));
    }

    private Rows show(ShowStatement show)
            throws SQLException
    {
        String pattern = show.getPattern() == null ? null : String.valueOf(Names.literal(show.getPattern()));
        Predicate<Map<String, Object>> matches = pattern == null ? row -> true : Predicates.of(likeExpression(pattern));
        Predicate<String> like = name -> matches.test(Map.of("name", name));
        switch (show.getShowType()) {
            case DATABASES:
                return single("Database", source.databases().stream().filter(like).collect(Collectors.toList()));
            case TABLES:
                String target = show.getDatabaseName() == null ? database : Names.unquote(show.getDatabaseName());
                if (target == null) {
                    throw new SQLException("No database selected");
                }
                return single("Table", source.tables(target).stream().filter(like).collect(Collectors.toList()));
            case COLUMNS:
                String name = show.getDatabaseName() == null ? show.getTableName() : show.getDatabaseName() + "." + show.getTableName();
                String[] table = existing(name);
                List<List<Object>> rows = new ArrayList<>();
                for (SourceColumn column : source.columns(table[0], table[1])) {
                    if (like.test(column.getName())) {
                        rows.add(Arrays.asList(column.getName(), column.getType(), column.isNullable() ? "YES" : "NO", column.isKey() ? "PRI" : "", null, column.getComment()));
                    }
                }
                return Rows.of(List.of("Field", "Type", "Null", "Key", "Default", "Comment"),
                        List.of("VARCHAR", "VARCHAR", "VARCHAR", "VARCHAR", "VARCHAR", "VARCHAR"), rows);
            default:
                throw new SQLFeatureNotSupportedException("SHOW " + show.getShowType());
        }
    }

    private static Expression likeExpression(String pattern)
    {
        Expression column = new Expression();
        column.setType(Expression.ExpressionType.COLUMN_REFERENCE);
        column.setValue("name");
        Expression literal = new Expression();
        literal.setType(Expression.ExpressionType.LITERAL);
        literal.setValue(pattern);
        Expression like = new Expression();
        like.setType(Expression.ExpressionType.BINARY_OP);
        like.setValue("LIKE");
        like.setChildren(List.of(column, literal));
        return like;
    }

    private static Rows single(String label, List<String> names)
    {
        return Rows.of(List.of(label), List.of("VARCHAR"), names.stream().map(name -> List.<Object>of(name)).collect(Collectors.toList()));
    }

    private Rows createTable(CreateTableStatement create)
            throws SQLException
    {
        String[] table = Names.table(create.getTableName(), database);
        if (!source.databases().contains(table[0])) {
            throw new SQLException("Unknown database " + table[0]);
        }
        if (source.tables(table[0]).contains(table[1])) {
            if (create.isIfNotExists()) {
                return Rows.updated(0);
            }
            throw new SQLException("Table " + table[0] + "." + table[1] + " already exists");
        }
        List<String> keys = new ArrayList<>();
        for (TableElement element : create.getColumns()) {
            if (element instanceof TableConstraint && ((TableConstraint) element).getType() == ConstraintType.PRIMARY_KEY) {
                Arrays.stream(((TableConstraint) element).getColumns()).map(Names::unquote).forEach(keys::add);
            }
        }
        List<SourceColumn> columns = new ArrayList<>();
        for (TableElement element : create.getColumns()) {
            if (element instanceof ColumnElement) {
                columns.add(column((ColumnElement) element, keys));
            }
        }
        source.createTable(table[0], table[1], columns);
        return Rows.updated(0);
    }

    private Rows alter(AlterTableStatement alter)
            throws SQLException
    {
        String[] table = existing(alter.getTableName());
        for (AlterTableStatement.Action action : alter.getActions()) {
            List<String> names = columnNames(table);
            switch (action.getType()) {
                case ADD_COLUMN:
                    String added = Names.unquote(action.getColumnName());
                    if (names.contains(added)) {
                        throw new SQLException("Column " + added + " already exists");
                    }
                    source.addColumn(table[0], table[1], column(action.getColumn(), List.of()));
                    break;
                case MODIFY_COLUMN:
                    known(names, Names.unquote(action.getColumnName()));
                    source.modifyColumn(table[0], table[1], column(action.getColumn(), List.of()));
                    break;
                case DROP_COLUMN:
                    source.dropColumn(table[0], table[1], known(names, Names.unquote(action.getColumnName())));
                    break;
                case RENAME_COLUMN:
                    String renamed = Names.unquote(action.getNewName());
                    if (names.contains(renamed)) {
                        throw new SQLException("Column " + renamed + " already exists");
                    }
                    source.renameColumn(table[0], table[1], known(names, Names.unquote(action.getColumnName())), renamed);
                    break;
                default:
                    throw new SQLFeatureNotSupportedException("Only ADD, MODIFY, DROP and RENAME COLUMN are supported in ALTER TABLE");
            }
        }
        return Rows.updated(0);
    }

    private Rows insert(InsertStatement insert)
            throws SQLException
    {
        if (insert.getSelect() != null) {
            throw new SQLFeatureNotSupportedException("INSERT ... SELECT is not supported");
        }
        String[] table = existing(insert.getTableName());
        List<String> names = columnNames(table);
        List<String> columns = new ArrayList<>();
        if (insert.getColumns() == null || insert.getColumns().isEmpty()) {
            columns.addAll(names);
        }
        else {
            for (String column : insert.getColumns()) {
                columns.add(known(names, Names.unquote(column)));
            }
        }
        List<List<Object>> rows = new ArrayList<>();
        for (List<Object> values : insert.getSimpleValues()) {
            if (values.size() != columns.size()) {
                throw new SQLException("Column count " + columns.size() + " does not match value count " + values.size());
            }
            rows.add(values.stream().map(Names::literal).collect(Collectors.toList()));
        }
        return Rows.updated(source.insert(table[0], table[1], columns, rows));
    }

    private String[] existing(String name)
            throws SQLException
    {
        String[] table = Names.table(name, database);
        if (!source.tables(table[0]).contains(table[1])) {
            throw new SQLException("Unknown table " + table[0] + "." + table[1]);
        }
        return table;
    }

    private List<String> columnNames(String[] table)
            throws SQLException
    {
        return source.columns(table[0], table[1]).stream().map(SourceColumn::getName).collect(Collectors.toList());
    }

    private static String known(List<String> names, String name)
            throws SQLException
    {
        if (!names.contains(name)) {
            throw new SQLException("Unknown column " + name);
        }
        return name;
    }

    private static Map<String, Object> named(List<String> names, List<Object> row)
    {
        Map<String, Object> named = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            named.put(names.get(i), i < row.size() ? row.get(i) : null);
        }
        return named;
    }

    private static SourceColumn column(ColumnElement element, List<String> keys)
    {
        String name = Names.unquote(element.getColumnName());
        boolean nullable = true;
        boolean key = keys.contains(name);
        String comment = "";
        if (element.getConstraints() != null) {
            for (ColumnConstraint constraint : element.getConstraints()) {
                if (constraint.getType() == ConstraintType.NOT_NULL) {
                    nullable = false;
                }
                else if (constraint.getType() == ConstraintType.PRIMARY_KEY) {
                    key = true;
                    nullable = false;
                }
            }
        }
        String type = element.getDataType() == null ? "VARCHAR" : element.getDataType().getBaseType().toUpperCase(Locale.ROOT);
        return new SourceColumn(name, type, nullable, key, comment);
    }
}
