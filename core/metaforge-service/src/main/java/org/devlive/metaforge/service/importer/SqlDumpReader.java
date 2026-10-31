package org.devlive.metaforge.service.importer;

import org.devlive.metaforge.spi.sql.SqlStatements;
import org.devlive.metaforge.spi.sql.TableDdl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a SQL dump (mysqldump style): CREATE TABLE statements give tables and columns, INSERT
 * statements give rows. Nothing is executed; other statements are counted as skipped.
 */
public final class SqlDumpReader
{
    private static final Pattern CREATE = Pattern.compile("(?is)^CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?((?:`[^`]+`\\.)?`?[\\w$]+`?)");
    private static final Pattern INSERT = Pattern.compile("(?is)^(?:INSERT|REPLACE)\\s+(?:IGNORE\\s+)?INTO\\s+((?:`[^`]+`\\.)?`?[\\w$]+`?)\\s*(\\(([^)]*)\\))?\\s*VALUES\\s*(.*)$");
    private static final Pattern COLUMN = Pattern.compile("^`((?:[^`]|``)*)`\\s+([^\\s,]+(?:\\s+unsigned)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION = Pattern.compile("(?i)(?:Server version|Distrib)\\s+([\\d.]+)");

    private final Map<String, ImportTable> tables = new LinkedHashMap<>();
    private int skipped;
    private String dialect = "";

    private SqlDumpReader()
    {
    }

    public static SqlDumpReader read(String script)
    {
        SqlDumpReader reader = new SqlDumpReader();
        Matcher version = VERSION.matcher(script.length() > 4096 ? script.substring(0, 4096) : script);
        if (version.find()) {
            reader.dialect = "MySQL " + version.group(1).replaceAll("^(\\d+\\.\\d+).*", "$1");
        }
        else if (script.contains("`")) {
            reader.dialect = "MySQL";
        }
        for (String statement : SqlStatements.split(script)) {
            String text = stripComments(statement).trim();
            if (text.isEmpty()) {
                continue;
            }
            Matcher create = CREATE.matcher(text);
            Matcher insert = INSERT.matcher(text);
            if (create.find()) {
                reader.create(unquote(create.group(1)), text);
            }
            else if (insert.find()) {
                reader.insert(unquote(insert.group(1)), insert.group(3), insert.group(4));
            }
            else {
                reader.skipped++;
            }
        }
        return reader;
    }

    public List<ImportTable> getTables()
    {
        return new ArrayList<>(tables.values());
    }

    public int getSkipped()
    {
        return skipped;
    }

    public String getDialect()
    {
        return dialect;
    }

    private void create(String name, String statement)
    {
        ImportTable table = tables.computeIfAbsent(name, ImportTable::new);
        String definition = statement.replaceAll(";\\s*$", "");
        table.setDefinition(definition);
        table.getColumns().clear();
        for (String column : TableDdl.parse(definition).columnDefinitions().values()) {
            Matcher matcher = COLUMN.matcher(column);
            if (matcher.find()) {
                table.getColumns().add(new ImportTable.ImportColumn(matcher.group(1).replace("``", "`"), matcher.group(2).toUpperCase(Locale.ROOT)));
            }
        }
    }

    private void insert(String name, String columnList, String values)
    {
        ImportTable table = tables.computeIfAbsent(name, ImportTable::new);
        if (columnList != null && table.getColumns().isEmpty()) {
            for (String column : columnList.split(",")) {
                table.getColumns().add(new ImportTable.ImportColumn(unquote(column.trim()), ""));
            }
        }
        table.getRows().addAll(tuples(values));
        if (table.getDefinition() == null) {
            TypeInference.infer(table);
        }
    }

    /** The value tuples of a VALUES clause: quoted text unescaped, NULL as null, numbers as text. */
    static List<List<Object>> tuples(String values)
    {
        List<List<Object>> rows = new ArrayList<>();
        List<Object> row = null;
        StringBuilder token = new StringBuilder();
        boolean quoted = false;
        boolean wasQuoted = false;
        for (int index = 0; index < values.length(); index++) {
            char current = values.charAt(index);
            if (quoted) {
                if (current == '\\' && index + 1 < values.length()) {
                    token.append(unescape(values.charAt(++index)));
                }
                else if (current == '\'' && index + 1 < values.length() && values.charAt(index + 1) == '\'') {
                    token.append('\'');
                    index++;
                }
                else if (current == '\'') {
                    quoted = false;
                }
                else {
                    token.append(current);
                }
                continue;
            }
            if (current == '(' && row == null) {
                row = new ArrayList<>();
                token.setLength(0);
                wasQuoted = false;
            }
            else if (current == '\'' && row != null) {
                quoted = true;
                wasQuoted = true;
            }
            else if ((current == ',' || current == ')') && row != null) {
                row.add(value(token.toString().trim(), wasQuoted));
                token.setLength(0);
                wasQuoted = false;
                if (current == ')') {
                    rows.add(row);
                    row = null;
                }
            }
            else if (row != null) {
                token.append(current);
            }
        }
        return rows;
    }

    private static Object value(String text, boolean quoted)
    {
        if (quoted) {
            return text;
        }
        return "NULL".equalsIgnoreCase(text) ? null : text;
    }

    private static char unescape(char next)
    {
        switch (next) {
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case '0':
                return '\0';
            default:
                return next;
        }
    }

    private static String unquote(String name)
    {
        String last = name.contains("`.`") ? name.substring(name.lastIndexOf("`.`") + 2) : name;
        return last.replaceAll("^`|`$", "").replace("``", "`");
    }

    private static String stripComments(String statement)
    {
        StringBuilder text = new StringBuilder();
        for (String line : statement.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.startsWith("#") || (trimmed.startsWith("/*!") && trimmed.endsWith("*/"))) {
                continue;
            }
            text.append(line).append('\n');
        }
        return text.toString();
    }
}
