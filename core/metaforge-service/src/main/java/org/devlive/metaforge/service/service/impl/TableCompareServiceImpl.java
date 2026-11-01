package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.body.TableCompareBody;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.TableCompareService;
import org.devlive.metaforge.service.service.TableExportService.ExportFile;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.compare.DataDiff;
import org.devlive.metaforge.spi.compare.StructureDiff;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.sql.TableDdl;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares two tables of one source: their columns, and their rows paired by key, and turns the
 * differences into statements that make the target match the source, or into a CSV report.
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class TableCompareServiceImpl
        implements TableCompareService
{
    /** Rows read per side at most; larger tables are compared a range at a time. */
    static final int MAX_ROWS = 200_000;
    /** Differences returned to the page; exports cover all of them. */
    static final int PAGE_ROWS = 500;

    private final PluginManager pluginManager;
    private final SourceRepository repository;

    public TableCompareServiceImpl(PluginManager pluginManager, SourceRepository repository)
    {
        this.pluginManager = pluginManager;
        this.repository = repository;
    }

    /** The source's adapter with a configure returning objects (for metadata) and one returning raw rows. */
    private static final class Source
    {
        private final PluginService service;
        private final RuntimeConfigure json;
        private final RuntimeConfigure raw;

        private Source(PluginService service, RuntimeConfigure json, RuntimeConfigure raw)
        {
            this.service = service;
            this.json = json;
            this.raw = raw;
        }
    }

    private interface Work<T>
    {
        CommonResponse<T> run(Source source);
    }

    private <T> CommonResponse<T> with(String code, TableCompareBody body, Set<String> capabilities, Work<T> work)
    {
        if (isBlank(body.getSourceDatabase()) || isBlank(body.getSourceTable()) || isBlank(body.getTargetDatabase()) || isBlank(body.getTargetTable())) {
            return CommonResponse.failure("Choose the source and the target table");
        }
        Optional<SourceEntity> entity = repository.findByCode(code);
        if (!entity.isPresent()) {
            return CommonResponse.failure(String.format("Resource [ %s ] not found", code));
        }
        Optional<Plugin> plugin = pluginManager.getPlugin(entity.get().getType());
        if (!plugin.isPresent()) {
            return CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.get().getType()));
        }
        PluginService service = plugin.get().getService(PluginService.class);
        Optional<String> missing = capabilities.stream().filter(capability -> !service.managementCapabilities().supports(capability)).findFirst();
        if (missing.isPresent()) {
            return CommonResponse.failure("Source does not support " + missing.get());
        }
        try {
            return work.run(new Source(service, entity.get().toConfigure(pluginManager, plugin.get()), entity.get().toConfigure("NoneConvert", pluginManager, plugin.get())));
        }
        catch (RuntimeException error) {
            return CommonResponse.failure(error.getMessage() == null ? "Compare failed" : error.getMessage());
        }
    }

    @Override
    public CommonResponse<Map<String, Object>> structure(String code, TableCompareBody body)
    {
        return with(code, body, Set.of("column:read"), source -> {
            List<Map<String, Object>> rows = structureDiff(source, body);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("rows", rows);
            result.put("counts", counts(rows.stream().map(row -> String.valueOf(row.get("status"))).collect(Collectors.toList())));
            return CommonResponse.success(result);
        });
    }

    @Override
    public CommonResponse<Map<String, Object>> data(String code, TableCompareBody body)
    {
        return with(code, body, Set.of("column:read", "table:read"), source -> {
            DataCompare compare = dataDiff(source, body);
            List<DataDiff.Row> shown = visible(compare.result, body);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("columns", compare.columns);
            result.put("keys", compare.keys);
            result.put("compared", compare.compared);
            Map<String, Long> counts = counts(compare.result.getRows().stream().map(DataDiff.Row::getType).collect(Collectors.toList()));
            counts.put(DataDiff.SAME, compare.result.getSame());
            result.put("counts", counts);
            result.put("total", shown.size());
            result.put("truncated", shown.size() > PAGE_ROWS);
            result.put("rows", shown.stream().limit(PAGE_ROWS).map(TableCompareServiceImpl::rowMap).collect(Collectors.toList()));
            return CommonResponse.success(result);
        });
    }

    @Override
    public CommonResponse<ExportFile> export(String code, TableCompareBody body)
    {
        boolean data = "data".equals(body.getKind());
        boolean sql = !"csv".equals(body.getFormat());
        Set<String> capabilities = data ? Set.of("column:read", "table:read") : sql ? Set.of("column:read", "table:statement") : Set.of("column:read");
        return with(code, body, capabilities, source -> {
            String name = body.getSourceTable() + (data ? "-data" : "-structure") + (sql ? "-sync.sql" : "-diff.csv");
            String content;
            if (data) {
                DataCompare compare = dataDiff(source, body);
                content = sql ? dataSql(source.service, body, compare) : dataCsv(compare, visible(compare.result, body));
            }
            else {
                List<Map<String, Object>> rows = structureDiff(source, body);
                content = sql ? structureSql(source, body, rows) : structureCsv(rows);
            }
            // A byte order mark lets spreadsheet programs read the CSV as UTF-8.
            String text = sql ? content : "﻿" + content;
            return CommonResponse.success(new ExportFile(name, text.getBytes(StandardCharsets.UTF_8)));
        });
    }

    // ---- structure ----

    private List<Map<String, Object>> structureDiff(Source source, TableCompareBody body)
    {
        return StructureDiff.compare(columns(source, body.getSourceDatabase(), body.getSourceTable()), columns(source, body.getTargetDatabase(), body.getTargetTable()));
    }

    private static List<StructureDiff.Column> columns(Source source, String database, String table)
    {
        PluginService service = source.service;
        Response response = service.execute(source.raw, "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA = " + service.sqlLiteral(database) + " AND TABLE_NAME = " + service.sqlLiteral(table) + " ORDER BY ORDINAL_POSITION");
        List<List<Object>> rows = rows(response);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Table " + database + "." + table + " has no columns or does not exist");
        }
        return rows.stream()
                .map(row -> new StructureDiff.Column(text(row.get(0)), text(row.get(1)), "YES".equalsIgnoreCase(text(row.get(2))), row.get(3) == null ? null : text(row.get(3)),
                        text(row.get(4))))
                .collect(Collectors.toList());
    }

    /** ADD, MODIFY and DROP COLUMN statements built from the source table's own column definitions. */
    private static String structureSql(Source source, TableCompareBody body, List<Map<String, Object>> rows)
    {
        PluginService service = source.service;
        String target = service.sqlIdentifier(body.getTargetDatabase()) + "." + service.sqlIdentifier(body.getTargetTable());
        Map<String, String> definitions = TableDdl.parse(service.getTableDdl(source.json, body.getSourceDatabase(), body.getSourceTable(), "create").getContent())
                .columnDefinitions();
        List<String> statements = new ArrayList<>();
        String previous = null;
        for (Map<String, Object> row : rows) {
            String name = String.valueOf(row.get("name"));
            String status = String.valueOf(row.get("status"));
            if (StructureDiff.TARGET_ONLY.equals(status)) {
                statements.add("ALTER TABLE " + target + " DROP COLUMN " + service.sqlIdentifier(name));
                continue;
            }
            if (StructureDiff.SOURCE_ONLY.equals(status)) {
                statements.add("ALTER TABLE " + target + " ADD COLUMN " + definitions.get(name) + (previous == null ? " FIRST" : " AFTER " + service.sqlIdentifier(previous)));
            }
            else if (StructureDiff.CHANGED.equals(status)) {
                statements.add("ALTER TABLE " + target + " MODIFY COLUMN " + definitions.get(name));
            }
            previous = name;
        }
        String head = "-- Make " + target + " match " + service.sqlIdentifier(body.getSourceDatabase()) + "." + service.sqlIdentifier(body.getSourceTable()) + "\n";
        return statements.isEmpty() ? head + "-- The structures already match.\n" : head + String.join(";\n", statements) + ";\n";
    }

    private static String structureCsv(List<Map<String, Object>> rows)
    {
        StringBuilder csv = new StringBuilder(csvLine(List.of("status", "name", "source_type", "source_nullable", "source_default", "source_comment",
                "target_type", "target_nullable", "target_default", "target_comment", "changes")));
        for (Map<String, Object> row : rows) {
            List<Object> values = new ArrayList<>(List.of(row.get("status"), row.get("name")));
            for (String side : List.of("source", "target")) {
                Object column = row.get(side);
                Map<?, ?> map = column instanceof Map ? (Map<?, ?>) column : Map.of();
                values.add(map.get("type"));
                values.add(map.get("nullable"));
                values.add(map.get("defaultValue"));
                values.add(map.get("comment"));
            }
            values.add(String.join(" ", ((List<?>) row.get("changes")).stream().map(String::valueOf).collect(Collectors.toList())));
            csv.append(csvLine(values));
        }
        return csv.toString();
    }

    // ---- data ----

    private static final class DataCompare
    {
        private List<String> columns;
        private List<String> keys;
        private long compared;
        private DataDiff.Result result;
    }

    private DataCompare dataDiff(Source source, TableCompareBody body)
    {
        PluginService service = source.service;
        List<String> targetColumns = columns(source, body.getTargetDatabase(), body.getTargetTable()).stream().map(StructureDiff.Column::getName).collect(Collectors.toList());
        DataCompare compare = new DataCompare();
        compare.columns = columns(source, body.getSourceDatabase(), body.getSourceTable()).stream()
                .map(StructureDiff.Column::getName).filter(targetColumns::contains).collect(Collectors.toList());
        if (compare.columns.isEmpty()) {
            throw new IllegalArgumentException("The tables share no columns");
        }
        compare.keys = "fields".equals(body.getMode())
                ? body.getKeys().stream().filter(compare.columns::contains).distinct().collect(Collectors.toList())
                : service.formatPrimaryKeys(source.json, body.getSourceDatabase(), body.getSourceTable());
        if (compare.keys.isEmpty()) {
            throw new IllegalArgumentException("fields".equals(body.getMode()) ? "Choose the fields that pair the rows" : "The source table has no primary key; compare by fields");
        }
        if (!compare.columns.containsAll(compare.keys)) {
            throw new IllegalArgumentException("Both tables need the key columns " + compare.keys);
        }
        String list = compare.columns.stream().map(service::sqlIdentifier).collect(Collectors.joining(", "));
        String order = " ORDER BY " + compare.keys.stream().map(service::sqlIdentifier).collect(Collectors.joining(", "));
        boolean ranged = "range".equals(body.getScope());
        if (ranged && body.getLimit() < 1) {
            throw new IllegalArgumentException("Compare at least one row");
        }
        List<List<Object>> sourceRows = read(service, source.raw, "SELECT " + list + " FROM " + table(service, body.getSourceDatabase(), body.getSourceTable()) + order
                + (ranged ? " LIMIT " + Math.min(body.getLimit(), MAX_ROWS) + " OFFSET " + Math.max(0, body.getOffset()) : " LIMIT " + (MAX_ROWS + 1)));
        List<List<Object>> targetRows = new ArrayList<>();
        if (!ranged || !sourceRows.isEmpty()) {
            String where = ranged ? " WHERE " + between(service, compare, sourceRows.get(0), sourceRows.get(sourceRows.size() - 1)) : "";
            targetRows = read(service, source.raw, "SELECT " + list + " FROM " + table(service, body.getTargetDatabase(), body.getTargetTable()) + where + order
                    + " LIMIT " + (MAX_ROWS + 1));
        }
        compare.compared = sourceRows.size();
        compare.result = DataDiff.compare(compare.columns, compare.keys, sourceRows, targetRows, !body.isOnlyChanged());
        return compare;
    }

    private static List<List<Object>> read(PluginService service, RuntimeConfigure configure, String sql)
    {
        Response response = service.execute(configure, sql);
        if (response == null || !Boolean.TRUE.equals(response.getIsSuccessful())) {
            throw new IllegalArgumentException(response == null ? "Reading the rows failed" : response.getMessage());
        }
        List<List<Object>> rows = rows(response);
        if (rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("More than " + MAX_ROWS + " rows; compare a range instead");
        }
        return rows;
    }

    /** The key range of the source rows, as a row comparison so composite keys work too. */
    private static String between(PluginService service, DataCompare compare, List<Object> first, List<Object> last)
    {
        List<Integer> indexes = compare.keys.stream().map(compare.columns::indexOf).collect(Collectors.toList());
        String keys = compare.keys.stream().map(service::sqlIdentifier).collect(Collectors.joining(", "));
        String low = indexes.stream().map(index -> literal(service, first.get(index))).collect(Collectors.joining(", "));
        String high = indexes.stream().map(index -> literal(service, last.get(index))).collect(Collectors.joining(", "));
        return "(" + keys + ") >= (" + low + ") AND (" + keys + ") <= (" + high + ")";
    }

    private static List<DataDiff.Row> visible(DataDiff.Result result, TableCompareBody body)
    {
        return result.getRows().stream()
                .filter(row -> body.isIncludeMissing() || DataDiff.CHANGED.equals(row.getType()) || DataDiff.SAME.equals(row.getType()))
                .collect(Collectors.toList());
    }

    /** INSERT rows only the source has, UPDATE differing columns and DELETE rows only the target has. */
    private static String dataSql(PluginService service, TableCompareBody body, DataCompare compare)
    {
        String target = table(service, body.getTargetDatabase(), body.getTargetTable());
        String list = compare.columns.stream().map(service::sqlIdentifier).collect(Collectors.joining(", "));
        List<String> statements = new ArrayList<>();
        for (DataDiff.Row row : compare.result.getRows()) {
            String where = " WHERE " + condition(service, compare, row.getKey());
            if (DataDiff.CHANGED.equals(row.getType())) {
                List<Object> values = row.getSource();
                statements.add("UPDATE " + target + " SET " + row.getChanged().stream()
                        .map(column -> service.sqlIdentifier(column) + " = " + literal(service, values.get(compare.columns.indexOf(column))))
                        .collect(Collectors.joining(", ")) + where);
            }
            else if (body.isIncludeMissing() && DataDiff.SOURCE_ONLY.equals(row.getType())) {
                statements.add("INSERT INTO " + target + " (" + list + ") VALUES (" + row.getSource().stream().map(value -> literal(service, value)).collect(Collectors.joining(", ")) + ")");
            }
            else if (body.isIncludeMissing() && DataDiff.TARGET_ONLY.equals(row.getType())) {
                statements.add("DELETE FROM " + target + where);
            }
        }
        String head = "-- Make the rows of " + target + " match " + table(service, body.getSourceDatabase(), body.getSourceTable()) + "\n";
        return statements.isEmpty() ? head + "-- The rows already match.\n" : head + String.join(";\n", statements) + ";\n";
    }

    private static String dataCsv(DataCompare compare, List<DataDiff.Row> rows)
    {
        List<Object> header = new ArrayList<>(List.of("type"));
        compare.columns.forEach(column -> {
            header.add(column + " (source)");
            header.add(column + " (target)");
        });
        StringBuilder csv = new StringBuilder(csvLine(header));
        for (DataDiff.Row row : rows) {
            List<Object> values = new ArrayList<>(List.of(row.getType()));
            for (int index = 0; index < compare.columns.size(); index++) {
                values.add(row.getSource() == null ? "" : text(row.getSource().get(index)));
                values.add(row.getTarget() == null ? "" : text(row.getTarget().get(index)));
            }
            csv.append(csvLine(values));
        }
        return csv.toString();
    }

    private static String condition(PluginService service, DataCompare compare, List<Object> key)
    {
        List<String> parts = new ArrayList<>();
        for (int index = 0; index < compare.keys.size(); index++) {
            Object value = key.get(index);
            parts.add(service.sqlIdentifier(compare.keys.get(index)) + (value == null ? " IS NULL" : " = " + literal(service, value)));
        }
        return String.join(" AND ", parts);
    }

    // ---- helpers ----

    private static Map<String, Object> rowMap(DataDiff.Row row)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", row.getType());
        map.put("key", texts(row.getKey()));
        map.put("source", row.getSource() == null ? null : texts(row.getSource()));
        map.put("target", row.getTarget() == null ? null : texts(row.getTarget()));
        map.put("changed", row.getChanged());
        return map;
    }

    private static Map<String, Long> counts(List<String> statuses)
    {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : List.of(StructureDiff.CHANGED, StructureDiff.SOURCE_ONLY, StructureDiff.TARGET_ONLY, StructureDiff.SAME)) {
            counts.put(status, statuses.stream().filter(status::equals).count());
        }
        return counts;
    }

    @SuppressWarnings("unchecked")
    private static List<List<Object>> rows(Response response)
    {
        List<List<Object>> rows = new ArrayList<>();
        if (response == null || !Boolean.TRUE.equals(response.getIsSuccessful()) || response.getColumns() == null) {
            return rows;
        }
        for (Object row : response.getColumns()) {
            if (row instanceof List) {
                rows.add(new ArrayList<>((List<Object>) row));
            }
            else if (row instanceof JsonNode) {
                List<Object> values = new ArrayList<>();
                ((JsonNode) row).forEach(node -> values.add(node.isNull() ? null : node.asText()));
                rows.add(values);
            }
        }
        return rows;
    }

    private static String table(PluginService service, String database, String table)
    {
        return service.sqlIdentifier(database) + "." + service.sqlIdentifier(table);
    }

    private static String literal(PluginService service, Object value)
    {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? "1" : "0";
        }
        return service.sqlLiteral(String.valueOf(value));
    }

    private static List<String> texts(List<Object> values)
    {
        return values.stream().map(TableCompareServiceImpl::text).collect(Collectors.toList());
    }

    private static String text(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private static String csvLine(List<?> values)
    {
        return values.stream()
                .map(value -> {
                    String text = value == null ? "" : String.valueOf(value);
                    return text.matches("(?s).*[\",\\r\\n].*") ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
                })
                .collect(Collectors.joining(",")) + "\n";
    }
}
