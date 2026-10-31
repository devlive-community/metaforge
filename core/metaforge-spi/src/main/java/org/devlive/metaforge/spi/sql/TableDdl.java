package org.devlive.metaforge.spi.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Splits the output of SHOW CREATE TABLE into its parts and builds the statements derived from
 * them: the structure without keys, the indexes, the constraints and ALTER templates. MySQL prints
 * one definition per line, so every column, key and constraint keeps its exact text.
 */
public final class TableDdl
{
    private static final Pattern AUTO_INCREMENT = Pattern.compile("\\s*\\bAUTO_INCREMENT=\\d+");
    private static final Pattern COLUMN_NAME = Pattern.compile("^`((?:[^`]|``)*)`");

    private final String create;
    private final String header;
    private final List<String> columns = new ArrayList<>();
    private final List<String> indexes = new ArrayList<>();
    private final List<String> constraints = new ArrayList<>();
    private final String options;

    private TableDdl(String create)
    {
        this.create = create.trim();
        String[] lines = this.create.split("\\r?\\n");
        this.header = lines[0].trim();
        int close = 1;
        while (close < lines.length && !lines[close].startsWith(")")) {
            String definition = lines[close].trim().replaceAll(",$", "");
            String upper = definition.toUpperCase(Locale.ROOT);
            if (definition.startsWith("`")) {
                columns.add(definition);
            }
            else if (upper.startsWith("PRIMARY KEY") || upper.startsWith("UNIQUE ")) {
                indexes.add(definition);
                constraints.add(definition);
            }
            else if (upper.startsWith("KEY ") || upper.startsWith("INDEX ") || upper.startsWith("FULLTEXT ") || upper.startsWith("SPATIAL ")) {
                indexes.add(definition);
            }
            else if (!definition.isEmpty()) {
                constraints.add(definition);
            }
            close++;
        }
        // Partition clauses follow on their own lines and stay out of the derived statements.
        this.options = close < lines.length ? lines[close].substring(1).trim() : "";
    }

    public static TableDdl parse(String create)
    {
        if (create == null || create.trim().isEmpty()) {
            throw new IllegalArgumentException("The table statement is empty");
        }
        return new TableDdl(create);
    }

    /** The full statement as the database prints it. */
    public String create()
    {
        return create + ";";
    }

    /** Columns and table options only: no keys, constraints, partitions or AUTO_INCREMENT counter. */
    public String structure()
    {
        String body = columns.stream().map(column -> "  " + column).collect(Collectors.joining(",\n"));
        String tail = withoutCounter(options);
        return header + "\n" + body + "\n)" + (tail.isEmpty() ? "" : " " + tail) + ";";
    }

    /** One ALTER TABLE ... ADD per primary key, unique key and index. */
    public String indexes(String target)
    {
        return addEach(target, indexes);
    }

    /** One ALTER TABLE ... ADD per primary key, unique key, foreign key and check constraint. */
    public String constraints(String target)
    {
        return addEach(target, constraints);
    }

    /** One ALTER TABLE ... MODIFY COLUMN per column, each a template for changing that column. */
    public String alterColumns(String target)
    {
        return columns.stream().map(column -> "ALTER TABLE " + target + " MODIFY COLUMN " + column + ";").collect(Collectors.joining("\n"));
    }

    /** A single ALTER TABLE that restates every column and the table options, to sync another copy of the table. */
    public String alterTable(String target)
    {
        List<String> specs = columns.stream().map(column -> "MODIFY COLUMN " + column).collect(Collectors.toList());
        String tail = withoutCounter(options);
        if (!tail.isEmpty()) {
            specs.add(tail);
        }
        return specs.isEmpty() ? "" : "ALTER TABLE " + target + "\n  " + String.join(",\n  ", specs) + ";";
    }

    /** Every column definition as printed, keyed by column name in table order. */
    public Map<String, String> columnDefinitions()
    {
        Map<String, String> definitions = new LinkedHashMap<>();
        for (String column : columns) {
            Matcher matcher = COLUMN_NAME.matcher(column);
            if (matcher.find()) {
                definitions.put(matcher.group(1).replace("``", "`"), column);
            }
        }
        return definitions;
    }

    /** Column names in order, leaving out generated columns, which take no inserted values. */
    public List<String> insertableColumns()
    {
        List<String> names = new ArrayList<>();
        for (String column : columns) {
            Matcher matcher = COLUMN_NAME.matcher(column);
            if (matcher.find() && !column.toUpperCase(Locale.ROOT).contains(" GENERATED ALWAYS AS ")) {
                names.add(matcher.group(1).replace("``", "`"));
            }
        }
        return Collections.unmodifiableList(names);
    }

    private static String addEach(String target, List<String> definitions)
    {
        return definitions.stream().map(definition -> "ALTER TABLE " + target + " ADD " + definition + ";").collect(Collectors.joining("\n"));
    }

    /** Drops the AUTO_INCREMENT counter, looking only before the comment so its text stays untouched. */
    private static String withoutCounter(String options)
    {
        int comment = options.indexOf(" COMMENT=");
        String head = comment < 0 ? options : options.substring(0, comment);
        String rest = comment < 0 ? "" : options.substring(comment);
        return (AUTO_INCREMENT.matcher(head).replaceFirst("") + rest).trim();
    }
}
