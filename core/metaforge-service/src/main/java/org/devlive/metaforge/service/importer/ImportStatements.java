package org.devlive.metaforge.service.importer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Builds the statements of one table's import: CREATE TABLE, TRUNCATE and batched INSERTs. */
public final class ImportStatements
{
    private static final Pattern CREATE_NAME = Pattern.compile("(?is)^(CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?)((?:`[^`]+`\\.)?`?[\\w$]+`?)");
    private static final DateTimeFormatter OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Function<String, String> identifier;
    private final Function<String, String> literal;

    /** @param identifier quotes a name ({@code `name`}); @param literal quotes a text value ({@code 'text'}) */
    public ImportStatements(Function<String, String> identifier, Function<String, String> literal)
    {
        this.identifier = identifier;
        this.literal = literal;
    }

    private String target(String database, String table)
    {
        return identifier.apply(database) + "." + identifier.apply(table);
    }

    /** The source definition under the target name, or columns from the inferred types when the file has none. */
    public String create(String database, ImportTable source, String table)
    {
        if (source.getDefinition() != null) {
            Matcher matcher = CREATE_NAME.matcher(source.getDefinition().trim());
            if (matcher.find()) {
                return matcher.replaceFirst(Matcher.quoteReplacement(matcher.group(1) + target(database, table)));
            }
        }
        return "CREATE TABLE " + target(database, table) + " (\n  " + source.getColumns().stream()
                .map(column -> identifier.apply(column.getName()) + " " + column.getType())
                .collect(Collectors.joining(",\n  ")) + "\n)";
    }

    public String truncate(String database, String table)
    {
        return "TRUNCATE TABLE " + target(database, table);
    }

    /** One multi-row INSERT per batch over the mapped fields, the conflict strategy applied. */
    public List<String> inserts(String database, ImportTable source, ImportPlan.TablePlan plan, ImportPlan.Settings settings, String conflict)
    {
        List<ImportPlan.FieldMapping> mapped = plan.getMappings().stream()
                .filter(mapping -> mapping.getTarget() != null && !mapping.getTarget().isEmpty() && !"ignore".equals(mapping.getHandling()))
                .filter(mapping -> source.columnIndex(mapping.getSource()) >= 0)
                .collect(Collectors.toList());
        List<String> statements = new ArrayList<>();
        if (mapped.isEmpty()) {
            return statements;
        }
        String head = ("skip".equals(conflict) ? "INSERT IGNORE INTO " : "INSERT INTO ") + target(database, plan.getTarget()) + " ("
                + mapped.stream().map(mapping -> identifier.apply(mapping.getTarget())).collect(Collectors.joining(", ")) + ") VALUES\n  ";
        String tail = "upsert".equals(conflict)
                ? "\nON DUPLICATE KEY UPDATE " + mapped.stream().map(mapping -> identifier.apply(mapping.getTarget()) + " = VALUES(" + identifier.apply(mapping.getTarget()) + ")")
                .collect(Collectors.joining(", "))
                : "";
        int batch = Math.max(1, settings.getBatchSize());
        List<List<Object>> rows = source.getRows();
        for (int start = 0; start < rows.size(); start += batch) {
            List<String> values = new ArrayList<>();
            for (List<Object> row : rows.subList(start, Math.min(rows.size(), start + batch))) {
                values.add("(" + mapped.stream().map(mapping -> value(row, source.columnIndex(mapping.getSource()), mapping, settings)).collect(Collectors.joining(", ")) + ")");
            }
            statements.add(head + String.join(",\n  ", values) + tail);
        }
        return statements;
    }

    String value(List<Object> row, int index, ImportPlan.FieldMapping mapping, ImportPlan.Settings settings)
    {
        Object value = index < row.size() ? row.get(index) : null;
        if (value != null && String.valueOf(value).isEmpty() && "emptyToNull".equals(settings.getNullMode())) {
            value = null;
        }
        if (value == null) {
            return "nullToEmpty".equals(settings.getNullMode()) ? literal.apply("") : "NULL";
        }
        String text = String.valueOf(value);
        if ("date".equals(mapping.getHandling())) {
            text = normaliseDate(text, settings.getTimeFormat());
        }
        return literal.apply(text);
    }

    /** A date written in the given pattern (or ISO) as yyyy-MM-dd HH:mm:ss; unparseable text is kept. */
    static String normaliseDate(String text, String pattern)
    {
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ofPattern(pattern), DateTimeFormatter.ISO_LOCAL_DATE_TIME)) {
            try {
                return LocalDateTime.parse(text.trim(), formatter).format(OUTPUT);
            }
            catch (DateTimeParseException | IllegalArgumentException error) {
                // Try the next pattern.
            }
        }
        return text;
    }
}
