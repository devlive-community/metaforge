package org.devlive.metaforge.service.importer;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Column types for files without a definition (CSV, JSON ...), inferred from the values, and the
 * compatibility of a source type with a target column type.
 */
public final class TypeInference
{
    private static final Pattern INTEGER = Pattern.compile("-?\\d{1,18}");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+\\.\\d+");
    private static final Pattern DATETIME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?)?");

    private TypeInference()
    {
    }

    /** Fills empty column types from the table's rows. */
    public static void infer(ImportTable table)
    {
        for (int index = 0; index < table.getColumns().size(); index++) {
            ImportTable.ImportColumn column = table.getColumns().get(index);
            if (column.getType() == null || column.getType().isEmpty()) {
                column.setType(infer(table.getRows(), index));
            }
        }
    }

    static String infer(List<List<Object>> rows, int index)
    {
        boolean integer = true;
        boolean decimal = true;
        boolean datetime = true;
        boolean json = true;
        boolean seen = false;
        int longest = 0;
        for (List<Object> row : rows) {
            Object value = index < row.size() ? row.get(index) : null;
            if (value == null || String.valueOf(value).isEmpty()) {
                continue;
            }
            String text = String.valueOf(value);
            seen = true;
            longest = Math.max(longest, text.length());
            integer &= INTEGER.matcher(text).matches();
            decimal &= INTEGER.matcher(text).matches() || DECIMAL.matcher(text).matches();
            datetime &= DATETIME.matcher(text).matches();
            json &= (text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"));
        }
        if (!seen) {
            return "VARCHAR(255)";
        }
        if (integer) {
            return "BIGINT";
        }
        if (decimal) {
            return "DECIMAL(20,6)";
        }
        if (datetime) {
            return longest > 10 ? "DATETIME" : "DATE";
        }
        if (json) {
            return "JSON";
        }
        return longest > 1024 ? "TEXT" : "VARCHAR(" + Math.max(64, Integer.highestOneBit(Math.max(1, longest - 1)) << 1) + ")";
    }

    /** Integer, decimal, text, time, json or binary, from a SQL type. */
    public static String family(String type)
    {
        String base = type == null ? "" : type.toLowerCase(Locale.ROOT).replaceAll("\\(.*", "").replace(" unsigned", "").trim();
        switch (base) {
            case "tinyint":
            case "smallint":
            case "mediumint":
            case "int":
            case "integer":
            case "bigint":
            case "bit":
            case "bool":
            case "boolean":
                return "integer";
            case "decimal":
            case "numeric":
            case "float":
            case "double":
            case "real":
                return "decimal";
            case "date":
            case "datetime":
            case "timestamp":
            case "time":
            case "year":
                return "time";
            case "json":
                return "json";
            case "blob":
            case "tinyblob":
            case "mediumblob":
            case "longblob":
            case "binary":
            case "varbinary":
                return "binary";
            default:
                return "text";
        }
    }

    /** Whether values of the source type can be written into the target type without handling. */
    public static boolean compatible(String source, String target)
    {
        String from = family(source);
        String to = family(target);
        return from.equals(to) || "text".equals(to) || ("integer".equals(from) && "decimal".equals(to)) || ("text".equals(from) && !"binary".equals(to) && source.isEmpty());
    }
}
