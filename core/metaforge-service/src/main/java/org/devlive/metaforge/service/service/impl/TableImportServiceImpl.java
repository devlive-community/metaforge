package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.convert.ConvertFilter;
import org.devlive.metaforge.convert.ConvertService;
import org.devlive.metaforge.convert.model.ConvertRequest;
import org.devlive.metaforge.convert.model.ConvertResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.service.common.SourceColumns;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.event.NotificationEventPublisher;
import org.devlive.metaforge.service.importer.ImportPlan;
import org.devlive.metaforge.service.importer.ImportStatements;
import org.devlive.metaforge.service.importer.ImportTable;
import org.devlive.metaforge.service.importer.SqlDumpReader;
import org.devlive.metaforge.service.importer.TypeInference;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.TableImportService;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * File import into a source's database. An upload is analysed once (SQL dumps by the built-in
 * reader, other formats by the installed convert plugins, ZIP archives entry by entry) and kept
 * for two hours under an import id; the wizard's plan is validated against the database and run as
 * a background job whose progress is polled.
 */
@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "REC_CATCH_EXCEPTION"}, justification = "Collaborators are Spring-managed singletons; a job records any failure")
public class TableImportServiceImpl
        implements TableImportService
{
    private static final long KEEP_MILLIS = TimeUnit.HOURS.toMillis(2);
    private static final int SAMPLE_ROWS = 4;

    private final PluginManager pluginManager;
    private final SourceRepository repository;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> jobs = new ConcurrentHashMap<>();
    private final ExecutorService runner = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "metaforge-import");
        thread.setDaemon(true);
        return thread;
    });

    public TableImportServiceImpl(PluginManager pluginManager, SourceRepository repository)
    {
        this.pluginManager = pluginManager;
        this.repository = repository;
    }

    private NotificationEventPublisher notifications = NotificationEventPublisher.NONE;

    @Autowired
    public void setNotifications(NotificationEventPublisher notifications)
    {
        this.notifications = notifications;
    }

    /** One analysed upload. */
    private static final class Session
    {
        private final String code;
        private final String database;
        private final String user;
        private final long created = System.currentTimeMillis();
        private final List<ImportTable> tables = new ArrayList<>();

        private Session(String code, String database, String user)
        {
            this.code = code;
            this.database = database;
            this.user = user;
        }
    }

    /** Sessions and jobs belong to the user who started them. */
    private static String currentUser()
    {
        return String.valueOf(UserDetailsService.getUser().getCode());
    }

    // ---- parsers ----

    @Override
    public CommonResponse<List<Map<String, Object>>> parsers()
    {
        List<Map<String, Object>> parsers = new ArrayList<>();
        parsers.add(parser("SqlDump", "sql"));
        parsers.add(parser("Zip", "zip"));
        for (PluginMetadata plugin : pluginManager.getPluginInfos()) {
            if (plugin.getType() == PluginType.CONVERT && !"NoneConvert".equals(plugin.getName())) {
                parsers.add(parser(plugin.getName(), extension(plugin.getName())));
            }
        }
        return CommonResponse.success(parsers);
    }

    private static Map<String, Object> parser(String name, String extension)
    {
        Map<String, Object> parser = new LinkedHashMap<>();
        parser.put("name", name);
        parser.put("extension", extension);
        return parser;
    }

    /** CsvConvert reads .csv, JsonConvert .json and so on. */
    private static String extension(String plugin)
    {
        return plugin.replaceAll("Convert$", "").toLowerCase(Locale.ROOT);
    }

    // ---- analyze ----

    @Override
    public CommonResponse<Map<String, Object>> analyze(String code, String database, MultipartFile file, String parser)
    {
        if (file == null || file.isEmpty()) {
            return CommonResponse.failure("Choose a file to import");
        }
        if (!repository.findByCode(code).isPresent()) {
            return CommonResponse.failure(String.format("Resource [ %s ] not found", code));
        }
        cleanup();
        try {
            String original = file.getOriginalFilename();
            String name = original == null ? "import" : new File(original).getName();
            String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
            Session session = new Session(code, database, currentUser());
            Map<String, Object> info = new LinkedHashMap<>();
            List<String> anomalies = new ArrayList<>();
            byte[] bytes = file.getBytes();
            if ("zip".equals(extension)) {
                info.put("parser", "Zip");
                try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            continue;
                        }
                        String inner = new File(entry.getName()).getName();
                        String innerExtension = inner.contains(".") ? inner.substring(inner.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
                        byte[] content = zip.readAllBytes();
                        try {
                            read(inner, innerExtension, content, null, session, info);
                        }
                        catch (IllegalArgumentException error) {
                            anomalies.add(inner + ": " + error.getMessage());
                        }
                    }
                }
            }
            else {
                read(name, extension, bytes, parser, session, info);
            }
            if (session.tables.isEmpty()) {
                return CommonResponse.failure("No table was found in the file");
            }
            for (ImportTable table : session.tables) {
                if (table.getColumns().isEmpty()) {
                    anomalies.add(table.getName() + ": no columns");
                }
                long ragged = table.getRows().stream().filter(row -> row.size() != table.getColumns().size()).count();
                if (ragged > 0) {
                    anomalies.add(table.getName() + ": " + ragged + " rows do not match the columns");
                }
            }
            String id = UUID.randomUUID().toString().replace("-", "");
            sessions.put(id, session);
            info.put("name", name);
            info.put("size", bytes.length);
            info.put("extension", extension);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("importId", id);
            result.put("file", info);
            result.put("tables", session.tables.stream().map(TableImportServiceImpl::describe).collect(Collectors.toList()));
            result.put("anomalies", anomalies);
            return CommonResponse.success(result);
        }
        catch (IOException | RuntimeException error) {
            log.warn("Analysing an import file failed", error);
            return CommonResponse.failure(error.getMessage() == null ? "The file could not be read" : error.getMessage());
        }
    }

    /** Reads one file with the parser chosen for it (or the one its extension names) into the session. */
    private void read(String name, String extension, byte[] bytes, String parser, Session session, Map<String, Object> info)
            throws IOException
    {
        Charset charset = charset(bytes);
        info.put("encoding", charset.name());
        String chosen = parser == null || parser.isEmpty() ? ("sql".equals(extension) ? "SqlDump" : pluginFor(extension)) : parser;
        if (chosen == null) {
            throw new IllegalArgumentException("No installed format reads ." + extension + " files");
        }
        info.put("parser", chosen);
        if ("SqlDump".equals(chosen)) {
            SqlDumpReader reader = SqlDumpReader.read(text(bytes, charset));
            info.put("dialect", reader.getDialect());
            info.put("skippedStatements", reader.getSkipped());
            session.tables.addAll(reader.getTables());
            return;
        }
        Optional<ConvertService> converter = ConvertFilter.filter(pluginManager, chosen);
        if (!converter.isPresent()) {
            throw new IllegalArgumentException("Format [ " + chosen + " ] is not installed");
        }
        String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        File directory = Files.createTempDirectory("metaforge-import").toFile();
        try {
            String fileExtension = converter.get().name().toLowerCase(Locale.ROOT);
            FileUtils.writeByteArrayToFile(new File(directory, base + "." + fileExtension), text(bytes, charset).getBytes(StandardCharsets.UTF_8));
            ConvertRequest request = new ConvertRequest();
            request.setName(base);
            request.setPath(directory.getAbsolutePath());
            request.setDelimiter("txt".equals(fileExtension) ? "\t" : ",");
            ConvertResponse response = converter.get().reader(request);
            if (!Boolean.TRUE.equals(response.getSuccessful())) {
                throw new IllegalArgumentException(response.getMessage() == null ? "The file could not be read" : response.getMessage());
            }
            ImportTable table = new ImportTable(base);
            List<String> headers = response.getHeaders().stream().map(String::valueOf).collect(Collectors.toList());
            headers.forEach(header -> table.getColumns().add(new ImportTable.ImportColumn(header, "")));
            for (Object row : response.getColumns()) {
                table.getRows().add(row(row, headers));
            }
            TypeInference.infer(table);
            session.tables.add(table);
        }
        finally {
            FileUtils.deleteQuietly(directory);
        }
    }

    private String pluginFor(String extension)
    {
        return pluginManager.getPluginInfos().stream()
                .filter(plugin -> plugin.getType() == PluginType.CONVERT && !"NoneConvert".equals(plugin.getName()) && extension(plugin.getName()).equals(extension))
                .map(PluginMetadata::getName)
                .findFirst()
                .orElse(null);
    }

    /** A row read by a convert plugin as values in header order: lists as they are, maps by header. */
    private static List<Object> row(Object row, List<String> headers)
    {
        List<Object> values = new ArrayList<>();
        if (row instanceof List) {
            values.addAll((List<?>) row);
        }
        else if (row instanceof Map) {
            headers.forEach(header -> values.add(plain(((Map<?, ?>) row).get(header))));
        }
        else if (row instanceof JsonNode) {
            headers.forEach(header -> values.add(plain(((JsonNode) row).get(header))));
        }
        return values;
    }

    private static Object plain(Object value)
    {
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            return node.isNull() ? null : node.isValueNode() ? node.asText() : node.toString();
        }
        return value;
    }

    /** UTF-8 when the bytes decode as UTF-8 (a byte order mark is dropped), GBK otherwise. */
    static Charset charset(byte[] bytes)
    {
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        }
        catch (CharacterCodingException error) {
            return Charset.forName("GBK");
        }
    }

    private static String text(byte[] bytes, Charset charset)
    {
        String text = new String(bytes, charset);
        return text.startsWith("﻿") ? text.substring(1) : text;
    }

    private static Map<String, Object> describe(ImportTable table)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", table.getName());
        map.put("rows", table.getRows().size());
        map.put("hasDefinition", table.getDefinition() != null);
        List<Map<String, Object>> columns = new ArrayList<>();
        for (int index = 0; index < table.getColumns().size(); index++) {
            int position = index;
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("name", table.getColumns().get(index).getName());
            column.put("type", table.getColumns().get(index).getType());
            column.put("sample", table.getRows().stream().limit(3).map(row -> position < row.size() && row.get(position) != null ? String.valueOf(row.get(position)) : "NULL")
                    .collect(Collectors.toList()));
            columns.add(column);
        }
        map.put("columns", columns);
        return map;
    }

    private void cleanup()
    {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> now - entry.getValue().created > KEEP_MILLIS);
        jobs.entrySet().removeIf(entry -> now - (long) entry.getValue().getOrDefault("startedAt", now) > KEEP_MILLIS && !"running".equals(entry.getValue().get("status")));
    }

    // ---- preview ----

    /** The source's adapter, the database's tables and their columns, for checking a plan. */
    private static final class Target
    {
        private PluginService service;
        private RuntimeConfigure configure;
        private String database;
        /** table -> [name, type, nullable, default, extra] per column, read through the plugin when a table is first checked. */
        private final Map<String, List<List<Object>>> columns = new HashMap<>();

        /** The table's columns; null when it does not exist. */
        List<List<Object>> columns(String table)
        {
            List<List<Object>> found = columns.computeIfAbsent(table, name -> {
                List<List<Object>> rows = new ArrayList<>();
                SourceColumns.of(service, configure, database, name).forEach(column -> rows.add(Arrays.asList(column.getName(), column.getType(),
                        column.isNullable() ? "YES" : "NO", column.getDefaultValue(), column.isAutoIncrement() ? "auto_increment" : "")));
                return rows;
            });
            return found.isEmpty() ? null : found;
        }
    }

    private Optional<Session> session(ImportPlan plan, String code, String database)
    {
        Session session = plan.getImportId() == null ? null : sessions.get(plan.getImportId());
        if (session == null || !session.code.equals(code) || !session.database.equals(database) || !session.user.equals(currentUser())) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    private CommonResponse<Target> target(String code, String database, ImportPlan plan)
    {
        Optional<SourceEntity> entity = repository.findByCode(code);
        if (!entity.isPresent()) {
            return CommonResponse.failure(String.format("Resource [ %s ] not found", code));
        }
        Optional<Plugin> plugin = pluginManager.getPlugin(entity.get().getType());
        if (!plugin.isPresent()) {
            return CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.get().getType()));
        }
        Target target = new Target();
        target.service = plugin.get().getService(PluginService.class);
        boolean creates = plan.getTables().stream().anyMatch(table -> "create".equals(table.getAction()));
        List<String> needed = new ArrayList<>(List.of("data:insert"));
        if (creates) {
            needed.add("table:create");
        }
        // A table may override the write mode, so every imported table's own mode counts.
        if (plan.getTables().stream().anyMatch(table -> !"skip".equals(table.getAction()) && "truncate".equals(writeMode(table, plan.getSettings())))) {
            needed.add("table:truncate");
        }
        Optional<String> missing = needed.stream().filter(capability -> !target.service.managementCapabilities().supports(capability)).findFirst();
        if (missing.isPresent()) {
            return CommonResponse.failure("Source does not support " + missing.get());
        }
        target.configure = entity.get().toConfigure("NoneConvert", pluginManager, plugin.get());
        target.database = database;
        return CommonResponse.success(target);
    }

    @Override
    public CommonResponse<Map<String, Object>> preview(String code, String database, ImportPlan plan)
    {
        Optional<Session> session = session(plan, code, database);
        if (!session.isPresent()) {
            return CommonResponse.failure("The analysed file has expired; choose it again");
        }
        CommonResponse<Target> target = target(code, database, plan);
        if (!Boolean.TRUE.equals(target.getStatus())) {
            return CommonResponse.failure(String.valueOf(target.getMessage()));
        }
        return CommonResponse.success(check(session.get(), database, plan, target.getData()));
    }

    /** Validates each planned table and builds the sample rows and the example SQL. */
    private Map<String, Object> check(Session session, String database, ImportPlan plan, Target target)
    {
        ImportStatements statements = statements(target.service);
        List<Map<String, Object>> tables = new ArrayList<>();
        int errors = 0;
        int warnings = 0;
        long rows = 0;
        // New tables are created before any row is written, so their statements come first.
        List<String> sql = new ArrayList<>();
        String example = null;
        Map<String, Object> sample = null;
        for (ImportPlan.TablePlan table : plan.getTables()) {
            if ("skip".equals(table.getAction())) {
                continue;
            }
            ImportTable source = find(session, table.getSource());
            List<Map<String, Object>> messages = new ArrayList<>();
            if (source == null) {
                messages.add(message("error", "sourceMissing", table.getSource()));
            }
            else {
                rows += source.getRows().size();
                validate(source, table, target, messages);
                if ("create".equals(table.getAction())) {
                    sql.add(statements.create(database, source, table.getTarget()));
                }
                if (sample == null) {
                    sample = sample(source, table);
                    List<String> inserts = statements.inserts(database, limited(source, 2), table, plan.getSettings(), conflict(table, plan.getSettings()));
                    if (!inserts.isEmpty()) {
                        example = inserts.get(0);
                    }
                }
            }
            long tableErrors = messages.stream().filter(item -> "error".equals(item.get("level"))).count();
            errors += tableErrors;
            warnings += messages.size() - tableErrors;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", table.getSource());
            row.put("target", table.getTarget());
            row.put("action", table.getAction());
            row.put("writeMode", table.getWriteMode() == null || table.getWriteMode().isEmpty() ? plan.getSettings().getWriteMode() : table.getWriteMode());
            row.put("rows", source == null ? 0 : source.getRows().size());
            row.put("status", tableErrors > 0 ? "error" : messages.isEmpty() ? "pass" : "warn");
            row.put("messages", messages);
            tables.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tables", tables);
        result.put("rows", rows);
        result.put("errors", errors);
        result.put("warnings", warnings);
        if (example != null) {
            sql.add(example);
        }
        result.put("sample", sample);
        result.put("sql", String.join(";\n", sql) + (sql.isEmpty() ? "" : ";"));
        return result;
    }

    private static void validate(ImportTable source, ImportPlan.TablePlan table, Target target, List<Map<String, Object>> messages)
    {
        String name = table.getTarget() == null ? "" : table.getTarget().trim();
        List<List<Object>> columns = name.isEmpty() ? null : target.columns(name);
        if (name.isEmpty()) {
            messages.add(message("error", "targetMissing", ""));
            return;
        }
        if ("create".equals(table.getAction())) {
            if (columns != null) {
                messages.add(message("error", "targetExists", name));
            }
            return;
        }
        if (columns == null) {
            messages.add(message("error", "targetMissing", name));
            return;
        }
        Map<String, List<Object>> byName = new LinkedHashMap<>();
        columns.forEach(column -> byName.put(String.valueOf(column.get(0)), column));
        List<String> mapped = new ArrayList<>();
        for (ImportPlan.FieldMapping mapping : table.getMappings()) {
            if (mapping.getTarget() == null || mapping.getTarget().isEmpty() || "ignore".equals(mapping.getHandling())) {
                continue;
            }
            List<Object> column = byName.get(mapping.getTarget());
            if (column == null) {
                messages.add(message("error", "fieldMissing", mapping.getTarget()));
                continue;
            }
            mapped.add(mapping.getTarget());
            int index = source.columnIndex(mapping.getSource());
            String sourceType = index < 0 ? "" : source.getColumns().get(index).getType();
            if (!"date".equals(mapping.getHandling()) && !TypeInference.compatible(sourceType, String.valueOf(column.get(1)))) {
                messages.add(message("warn", "typeMismatch", mapping.getSource() + " → " + mapping.getTarget()));
            }
        }
        if (mapped.isEmpty()) {
            messages.add(message("error", "noMapping", name));
        }
        byName.forEach((column, definition) -> {
            boolean required = "NO".equalsIgnoreCase(String.valueOf(definition.get(2))) && definition.get(3) == null
                    && !String.valueOf(definition.get(4)).toLowerCase(Locale.ROOT).contains("auto_increment");
            if (required && !mapped.contains(column)) {
                messages.add(message("error", "requiredUnmapped", column));
            }
        });
    }

    private static Map<String, Object> message(String level, String code, String subject)
    {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("level", level);
        message.put("code", code);
        message.put("subject", subject);
        return message;
    }

    private static Map<String, Object> sample(ImportTable source, ImportPlan.TablePlan table)
    {
        List<ImportPlan.FieldMapping> mapped = table.getMappings().stream()
                .filter(mapping -> mapping.getTarget() != null && !mapping.getTarget().isEmpty() && !"ignore".equals(mapping.getHandling()) && source.columnIndex(mapping.getSource()) >= 0)
                .collect(Collectors.toList());
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("table", source.getName());
        sample.put("columns", mapped.stream().map(ImportPlan.FieldMapping::getTarget).collect(Collectors.toList()));
        sample.put("rows", source.getRows().stream().limit(SAMPLE_ROWS)
                .map(row -> mapped.stream().map(mapping -> {
                    int index = source.columnIndex(mapping.getSource());
                    return index < row.size() && row.get(index) != null ? String.valueOf(row.get(index)) : "NULL";
                }).collect(Collectors.toList()))
                .collect(Collectors.toList()));
        return sample;
    }

    private static ImportTable limited(ImportTable source, int rows)
    {
        ImportTable copy = new ImportTable(source.getName());
        copy.getColumns().addAll(source.getColumns());
        copy.getRows().addAll(source.getRows().subList(0, Math.min(rows, source.getRows().size())));
        copy.setDefinition(source.getDefinition());
        return copy;
    }

    private static ImportTable find(Session session, String name)
    {
        return session.tables.stream().filter(table -> table.getName().equals(name)).findFirst().orElse(null);
    }

    private static String conflict(ImportPlan.TablePlan table, ImportPlan.Settings settings)
    {
        return table.getConflict() == null || table.getConflict().isEmpty() ? settings.getConflict() : table.getConflict();
    }

    private static String writeMode(ImportPlan.TablePlan table, ImportPlan.Settings settings)
    {
        return table.getWriteMode() == null || table.getWriteMode().isEmpty() ? settings.getWriteMode() : table.getWriteMode();
    }

    private static ImportStatements statements(PluginService service)
    {
        return new ImportStatements(service::sqlIdentifier, service::sqlLiteral);
    }

    // ---- execute ----

    @Override
    public CommonResponse<Map<String, Object>> execute(String code, String database, ImportPlan plan)
    {
        Optional<Session> session = session(plan, code, database);
        if (!session.isPresent()) {
            return CommonResponse.failure("The analysed file has expired; choose it again");
        }
        CommonResponse<Target> resolved = target(code, database, plan);
        if (!Boolean.TRUE.equals(resolved.getStatus())) {
            return CommonResponse.failure(String.valueOf(resolved.getMessage()));
        }
        Target target = resolved.getData();
        if (plan.getSettings().isCheckBefore() && ((Number) check(session.get(), database, plan, target).get("errors")).intValue() > 0) {
            return CommonResponse.failure("The plan has errors; fix them in the preview first");
        }
        String id = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> job = new ConcurrentHashMap<>();
        List<Map<String, Object>> progress = new ArrayList<>();
        for (ImportPlan.TablePlan table : plan.getTables()) {
            if (!"skip".equals(table.getAction())) {
                Map<String, Object> item = new ConcurrentHashMap<>();
                item.put("source", table.getSource());
                item.put("target", table.getTarget());
                item.put("status", "pending");
                item.put("rows", find(session.get(), table.getSource()) == null ? 0 : find(session.get(), table.getSource()).getRows().size());
                item.put("written", 0L);
                item.put("skipped", 0L);
                progress.add(item);
            }
        }
        job.put("id", id);
        job.put("user", session.get().user);
        job.put("status", "running");
        job.put("startedAt", System.currentTimeMillis());
        job.put("tables", progress);
        jobs.put(id, job);
        runner.submit(() -> run(session.get(), database, plan, target, job, progress));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", id);
        return CommonResponse.success(result);
    }

    private void run(Session session, String database, ImportPlan plan, Target target, Map<String, Object> job, List<Map<String, Object>> progress)
    {
        ImportStatements statements = statements(target.service);
        AtomicBoolean stopped = new AtomicBoolean(false);
        List<ImportPlan.TablePlan> tables = plan.getTables().stream().filter(table -> !"skip".equals(table.getAction())).collect(Collectors.toList());
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(4, plan.getSettings().getParallel())));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < tables.size(); index++) {
                ImportPlan.TablePlan table = tables.get(index);
                Map<String, Object> item = progress.get(index);
                futures.add(pool.submit(() -> {
                    if (stopped.get()) {
                        item.put("status", "cancelled");
                        return;
                    }
                    item.put("status", "running");
                    try {
                        importTable(session, database, plan, target, statements, table, item);
                        item.put("status", "done");
                    }
                    catch (RuntimeException error) {
                        item.put("status", "failed");
                        item.put("message", error.getMessage() == null ? "Import failed" : error.getMessage());
                        if (!plan.getSettings().isContinueOnError()) {
                            stopped.set(true);
                        }
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }
        catch (Exception error) {
            job.put("message", error.getMessage() == null ? "Import failed" : error.getMessage());
        }
        finally {
            pool.shutdown();
            boolean failed = progress.stream().anyMatch(item -> "failed".equals(item.get("status")));
            job.put("status", failed ? "failed" : "done");
            job.put("finishedAt", System.currentTimeMillis());
            notifyFinished(session, database, failed, progress);
        }
    }

    private void notifyFinished(Session session, String database, boolean failed, List<Map<String, Object>> progress)
    {
        long done = progress.stream().filter(item -> "done".equals(item.get("status"))).count();
        String reason = progress.stream().filter(item -> "failed".equals(item.get("status")))
                .map(item -> item.get("target") + ": " + item.get("message")).findFirst().orElse("");
        notifications.publishTo(session.user, failed ? NotificationScene.IMPORT_FAILED : NotificationScene.IMPORT_SUCCEEDED,
                String.format(failed ? "导入到 %s 失败" : "导入到 %s 完成", database),
                failed ? String.format("%d/%d 张表导入成功。%s", done, progress.size(), reason)
                        : String.format("%d 张表已导入 %s。", done, database),
                EntityType.SOURCE, session.code, database);
    }

    private static void importTable(Session session, String database, ImportPlan plan, Target target, ImportStatements statements, ImportPlan.TablePlan table,
            Map<String, Object> item)
    {
        ImportTable source = find(session, table.getSource());
        if (source == null) {
            throw new IllegalArgumentException("The file has no table " + table.getSource());
        }
        String mode = writeMode(table, plan.getSettings());
        List<String> executed = new ArrayList<>();
        if ("create".equals(table.getAction())) {
            run(target, statements.create(database, source, table.getTarget()), executed);
        }
        if ("structure".equals(mode)) {
            item.put("statements", executed);
            return;
        }
        if ("truncate".equals(mode) && !"create".equals(table.getAction())) {
            run(target, statements.truncate(database, table.getTarget()), executed);
        }
        String conflict = conflict(table, plan.getSettings());
        List<String> inserts = statements.inserts(database, source, table, plan.getSettings(), conflict);
        long written = 0;
        if (plan.getSettings().isTransactional() && inserts.size() > 1) {
            // One script runs as one transaction in the JDBC adapter.
            written = run(target, String.join(";\n", inserts), executed);
        }
        else {
            for (String insert : inserts) {
                written += run(target, insert, executed);
                item.put("written", written);
            }
        }
        item.put("written", written);
        if ("skip".equals(conflict) && plan.getSettings().isLogSkipped()) {
            item.put("skipped", Math.max(0, source.getRows().size() - written));
        }
        item.put("statements", executed.stream().map(statement -> statement.length() > 2000 ? statement.substring(0, 2000) + " …" : statement).collect(Collectors.toList()));
    }

    /** Runs one statement or script and returns the affected rows the adapter reports. */
    private static long run(Target target, String sql, List<String> executed)
    {
        executed.add(sql);
        Response response = target.service.execute(target.configure, sql);
        if (response == null || !Boolean.TRUE.equals(response.getIsSuccessful())) {
            throw new IllegalArgumentException(response == null ? "The statement failed" : response.getMessage());
        }
        long affected = 0;
        for (Object row : response.getColumns() == null ? List.of() : response.getColumns()) {
            Object value = row instanceof List && !((List<?>) row).isEmpty() ? ((List<?>) row).get(0) : null;
            if (value instanceof Number) {
                affected += ((Number) value).longValue();
            }
        }
        return affected;
    }

    @Override
    public CommonResponse<Map<String, Object>> job(String id)
    {
        Map<String, Object> job = jobs.get(id);
        if (job == null || !String.valueOf(job.get("user")).equals(currentUser())) {
            return CommonResponse.failure("Import job not found");
        }
        Map<String, Object> copy = new LinkedHashMap<>(job);
        copy.remove("user");
        return CommonResponse.success(copy);
    }
}
