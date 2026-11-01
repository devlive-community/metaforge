package org.devlive.metaforge.service.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** The SQL a database export runs: row selects with fields, filters and pages, and the schema file. */
public final class ExportStatements
{
    public static final Set<String> OPERATORS = Set.of("=", "!=", ">", ">=", "<", "<=", "LIKE", "NOT LIKE", "IS NULL", "IS NOT NULL");
    private static final Pattern INDEX_LINE = Pattern.compile("^\\s*(UNIQUE |FULLTEXT |SPATIAL )?(KEY|INDEX)\\s|^\\s*CONSTRAINT\\s", Pattern.CASE_INSENSITIVE);

    private final UnaryOperator<String> identifier;
    private final UnaryOperator<String> literal;

    public ExportStatements(UnaryOperator<String> identifier, UnaryOperator<String> literal)
    {
        this.identifier = identifier;
        this.literal = literal;
    }

    public String target(String database, String table)
    {
        return identifier.apply(database) + "." + identifier.apply(table);
    }

    /** WHERE clause of a table's filters, empty without filters. */
    public String where(List<ExportPlan.Filter> filters)
    {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        List<String> conditions = new ArrayList<>();
        for (ExportPlan.Filter filter : filters) {
            String operator = filter.getOperator() == null ? "=" : filter.getOperator().toUpperCase(Locale.ROOT);
            if (!OPERATORS.contains(operator)) {
                throw new IllegalArgumentException("Unsupported operator " + filter.getOperator());
            }
            String column = identifier.apply(filter.getColumn());
            conditions.add(operator.startsWith("IS") ? column + " " + operator : column + " " + operator + " " + literal.apply(filter.getValue() == null ? "" : filter.getValue()));
        }
        return " WHERE " + String.join(" AND ", conditions);
    }

    /** The select of one page (limit &gt; 0) or of every row (limit &lt;= 0). */
    public String select(String database, ExportPlan.TablePlan table, long limit, long offset)
    {
        String fields = table.getFields() == null || table.getFields().isEmpty() ? "*"
                : table.getFields().stream().map(identifier).collect(Collectors.joining(", "));
        String sql = "SELECT " + fields + " FROM " + target(database, table.getName()) + where(table.getFilters());
        return limit > 0 ? sql + " LIMIT " + limit + " OFFSET " + offset : sql;
    }

    public String count(String database, ExportPlan.TablePlan table)
    {
        return "SELECT COUNT(*) FROM " + target(database, table.getName()) + where(table.getFilters());
    }

    /** The schema.sql part of one table: DROP and CREATE as chosen, secondary indexes and constraints left out on request. */
    public String schema(String database, String table, String create, ExportPlan.Structure structure)
    {
        StringBuilder sql = new StringBuilder();
        if (structure.isDrop()) {
            sql.append("DROP TABLE IF EXISTS ").append(target(database, table)).append(";\n");
        }
        if (structure.isCreate() && create != null && !create.isEmpty()) {
            String statement = structure.isIndexes() ? create : withoutIndexes(create);
            sql.append(statement.trim().replaceAll(";+$", "")).append(";\n");
        }
        return sql.toString();
    }

    /** CREATE TABLE without KEY, INDEX and CONSTRAINT lines; columns and the primary key stay. */
    static String withoutIndexes(String create)
    {
        List<String> lines = new ArrayList<>();
        for (String line : create.split("\n")) {
            if (!INDEX_LINE.matcher(line).find()) {
                lines.add(line);
            }
        }
        // The line before the closing parenthesis must not end with a comma any more.
        for (int i = lines.size() - 1; i > 0; i--) {
            if (lines.get(i).trim().startsWith(")")) {
                String previous = lines.get(i - 1);
                lines.set(i - 1, previous.replaceAll(",\\s*$", ""));
                break;
            }
        }
        return String.join("\n", lines);
    }

    /** The file of a table, with the plugin's extension. */
    public static String fileName(String table, String extension)
    {
        return table + "." + extension;
    }

    /** A name for a file: path separators and reserved characters replaced. */
    public static String safeName(String name, String fallback)
    {
        String safe = name == null ? "" : name.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return safe.isEmpty() ? fallback : safe;
    }

    public static boolean known(Collection<String> names, String name)
    {
        return name != null && names.contains(name);
    }
}
