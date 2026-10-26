package org.devlive.metaforge.common.sql;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.sql.builder.AlterBuilder;
import org.devlive.metaforge.common.sql.builder.DeleteBuilder;
import org.devlive.metaforge.common.sql.builder.DropBuilder;
import org.devlive.metaforge.common.sql.builder.InsertBuilder;
import org.devlive.metaforge.common.sql.builder.SelectBuilder;
import org.devlive.metaforge.common.sql.builder.ShowBuilder;
import org.devlive.metaforge.common.sql.builder.TruncateBuilder;
import org.devlive.metaforge.common.sql.builder.UpdateBuilder;
import org.devlive.metaforge.common.sql.configure.SqlBody;
import org.devlive.metaforge.common.sql.configure.SqlColumn;
import org.devlive.metaforge.common.sql.configure.SqlOperator;

import java.util.List;
import java.util.stream.Collectors;

public class SqlBuilder
{
    private final SqlBody configure;

    public SqlBuilder(final SqlBody configure)
    {
        this.configure = new SqlBody(configure.getDatabase(), configure.getTable(), configure.getColumns(), configure.getGroups(), configure.getOrders(), configure.getLimit(),
                configure.getOffset(), configure.getWhere(), configure.getType(), configure.getValue(), configure.getCondition(), configure.isPreview());
    }

    public String getSql()
    {
        String sql = "";
        switch (configure.getType()) {
            case SELECT:
                sql = getSelect();
                break;
            case UPDATE:
                sql = getUpdate();
                break;
            case DELETE:
                sql = getDelete();
                break;
            case ALTER:
                sql = getAlter();
                break;
            case SHOW:
                sql = getShowCreateTable();
                break;
            case INSERT:
                sql = getInsert();
                break;
            case TRUNCATE:
                sql = getTruncateTable();
                break;
            case DROP:
                sql = getDropTable();
                break;
            default:
                Preconditions.checkArgument(false, "Not support type");
        }
        return sql;
    }

    private String applyDatabaseAndTable()
    {
        Preconditions.checkArgument(StringUtils.isNotEmpty(configure.getTable()), "The table name must be specified");
        if (StringUtils.isNotEmpty(configure.getDatabase())) {
            return String.format("`%s`.`%s`", configure.getDatabase(), configure.getTable());
        }
        return configure.getTable();
    }

    private List<String> applySelectColumns()
    {
        Preconditions.checkArgument(ArrayUtils.isNotEmpty(configure.getColumns().toArray(new SqlColumn[0])), "The columns must be specified");
        return configure.getColumns().stream()
                .map(item -> {
                    String value = String.format("%s", item.getColumn());
                    if (item.getExpression() != null) {
                        value = String.format("%s(%s)", item.getExpression(), value);
                    }
                    if (item.getAlias() != null) {
                        value = String.format("%s AS `%s`", value, item.getAlias());
                    }
                    return value;
                })
                .collect(Collectors.toList());
    }

    private List<String> applyOrderByColumns()
    {
        return configure.getOrders().stream()
                .map(v -> {
                    if (StringUtils.isNotEmpty(v.getExpression())) {
                        return String.format("%s(%s) %s", v.getExpression(), v.getColumn(), v.getOrder());
                    }
                    return String.format("%s %s", v.getColumn(), v.getOrder());
                })
                .collect(Collectors.toList());
    }

    private List<String> applyGroupByColumns()
    {
        return configure.getGroups().stream()
                .map(v -> String.join(" ", String.format("%s", v.getColumn())))
                .collect(Collectors.toList());
    }

    private String applyWhere()
    {
        return configure.getWhere()
                .stream()
                .map(column -> {
                    String value = StringEscapeUtils.escapeSql(String.valueOf(column.getValue()));
                    if (column.getOperator().equals(SqlOperator.LIKE) || column.getOperator().equals(SqlOperator.NLIKE)) {
                        value = StringEscapeUtils.escapeSql(String.format("%%%s%%", value));
                    }
                    else if (column.getOperator().equals(SqlOperator.NULL) || column.getOperator().equals(SqlOperator.NNULL)) {
                        return String.format("`%s` %s", column.getColumn(), column.getOperator().getSymbol());
                    }
                    return String.format("`%s` %s '%s'", column.getColumn(), column.getOperator().getSymbol(), value);
                })
                .collect(Collectors.joining(configure.getCondition()));
    }

    private String getSelect()
    {
        SelectBuilder.BEGIN();
        SelectBuilder.SELECT(applySelectColumns());
        SelectBuilder.FROM(applyDatabaseAndTable());

        if (ObjectUtils.isNotEmpty(configure.getOrders())) {
            SelectBuilder.ORDER_BY(applyOrderByColumns());
        }

        if (configure.getGroups() != null && !configure.getGroups().isEmpty()) {
            SelectBuilder.GROUP_BY(applyGroupByColumns());
        }

        if (configure.getWhere() != null && !configure.getWhere().isEmpty()) {
            SelectBuilder.WHERE(applyWhere());
        }

        if (configure.getLimit() != null) {
            SelectBuilder.LIMIT(configure.getLimit());
        }

        if (configure.getOffset() != null) {
            SelectBuilder.OFFSET(configure.getOffset());
        }
        return SelectBuilder.SQL();
    }

    /**
     * Generates a list of update columns based on the configured columns.
     *
     * @return A list of update columns in the format "`column` = 'value'".
     */
    private List<String> applyUpdateColumns()
    {
        Preconditions.checkArgument(ArrayUtils.isNotEmpty(configure.getColumns().toArray(new SqlColumn[0])), "The columns must be specified");
        return configure.getColumns()
                .stream()
                .map(column -> String.format("`%s` = '%s'", column.getColumn(), StringEscapeUtils.escapeSql(String.valueOf(column.getValue()))))
                .collect(Collectors.toList());
    }

    public List<String> applyUpdateWhere()
    {
        return configure.getWhere()
                .stream()
                .map(column -> String.format("`%s` %s '%s'", column.getColumn(), column.getOperator().getSymbol(), StringEscapeUtils.escapeSql(String.valueOf(column.getValue()))))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the update SQL statement based on the specified database and table,
     * update columns, and optional WHERE clause.
     *
     * @return The update SQL statement.
     */
    private String getUpdate()
    {
        UpdateBuilder.BEGIN();
        UpdateBuilder.UPDATE(applyDatabaseAndTable());

        UpdateBuilder.SET(applyUpdateColumns());

        if (configure.getWhere() != null) {
            UpdateBuilder.WHERE(applyUpdateWhere());
        }

        UpdateBuilder.END();
        return UpdateBuilder.SQL();
    }

    /**
     * Generates a SQL DELETE statement based on the provided configuration.
     *
     * @return The generated SQL DELETE statement.
     */
    private String getDelete()
    {
        DeleteBuilder.BEGIN();
        DeleteBuilder.DELETE_FROM(applyDatabaseAndTable());

        if (configure.getWhere() != null) {
            DeleteBuilder.WHERE(applyUpdateWhere());
        }

        DeleteBuilder.END();
        return DeleteBuilder.SQL();
    }

    /**
     * Generate the function comment for the given function body in a markdown code block with the correct language syntax.
     *
     * @return The generated function comment.
     */
    private String getAlter()
    {
        AlterBuilder.BEGIN();
        AlterBuilder.ALTER_TABLE(applyDatabaseAndTable());
        AlterBuilder.AUTO_INCREMENT(String.valueOf(configure.getValue()));
        return AlterBuilder.SQL();
    }

    /**
     * Generates the SHOW CREATE TABLE SQL statement for the specified database and table.
     *
     * @return the SHOW CREATE TABLE SQL statement
     */
    private String getShowCreateTable()
    {
        ShowBuilder.BEGIN();
        ShowBuilder.SHOW_CREATE_TABLE(applyDatabaseAndTable());
        return ShowBuilder.SQL();
    }

    /**
     * Generates the SQL INSERT statement for the given database and table,
     * using the configured columns and values.
     *
     * @return The SQL INSERT statement.
     */
    public String getInsert()
    {
        InsertBuilder.BEGIN();
        InsertBuilder.INSERT_INTO(applyDatabaseAndTable());
        List<String> columns = Lists.newArrayList();
        List<String> values = Lists.newArrayList();
        configure.getColumns()
                .forEach(column -> {
                    columns.add(column.getColumn());
                    values.add(String.valueOf(column.getValue()));
                });
        InsertBuilder.INTO_COLUMNS(columns.toArray(new String[0]));
        InsertBuilder.INTO_VALUES(values.toArray(new String[0]));
        InsertBuilder.END();
        return InsertBuilder.SQL();
    }

    /**
     * Generate the function comment for the given function body.
     *
     * @return a string representation of the SQL query for truncating the table
     */
    public String getTruncateTable()
    {
        TruncateBuilder.BEGIN();
        TruncateBuilder.TRUNCATE(applyDatabaseAndTable());
        return TruncateBuilder.SQL();
    }

    /**
     * Retrieves the SQL statement for dropping a table.
     *
     * @return the SQL statement for dropping a table
     */
    public String getDropTable()
    {
        DropBuilder.BEGIN();
        DropBuilder.DROP(applyDatabaseAndTable());
        return DropBuilder.SQL();
    }
}
