package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.convert.ConvertFilter;
import org.devlive.metaforge.convert.ConvertService;
import org.devlive.metaforge.convert.model.ConvertChoice;
import org.devlive.metaforge.convert.model.ConvertOption;
import org.devlive.metaforge.convert.model.ConvertRequest;
import org.devlive.metaforge.convert.model.ConvertResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.service.common.SourceColumns;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.exporter.ExportPlan;
import org.devlive.metaforge.service.exporter.ExportStatements;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.DatabaseExportService;
import org.devlive.metaforge.service.service.TableExportService;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports tables of one database: chosen fields and rows of each table through an installed
 * convert plugin with its declared settings, plus a schema.sql of their structure, packed into
 * one ZIP (or left as the single file) by a background job the page follows and downloads.
 */
@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "REC_CATCH_EXCEPTION"}, justification = "Collaborators are Spring-managed singletons; a table's failure of any kind is reported on the table")
public class DatabaseExportServiceImpl
        implements DatabaseExportService
{
    private static final String RAW = "NoneConvert";
    private static final String SCHEMA = "schema.sql";
    private static final long KEEP = Duration.ofHours(2).toMillis();

    private final PluginManager pluginManager;
    private final SourceRepository repository;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final ExecutorService runner = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "metaforge-export");
        thread.setDaemon(true);
        return thread;
    });

    public DatabaseExportServiceImpl(PluginManager pluginManager, SourceRepository repository)
    {
        this.pluginManager = pluginManager;
        this.repository = repository;
    }

    /** Jobs belong to the user who started them. */
    private static String currentUser()
    {
        return String.valueOf(UserDetailsService.getUser().getCode());
    }

    // ---- formats ----

    @Override
    public CommonResponse<List<Map<String, Object>>> formats()
    {
        List<Map<String, Object>> formats = new ArrayList<>();
        for (PluginMetadata plugin : pluginManager.getPluginInfos()) {
            if (plugin.getType() != PluginType.CONVERT || RAW.equals(plugin.getName())) {
                continue;
            }
            Optional<ConvertService> converter = ConvertFilter.filter(pluginManager, plugin.getName());
            if (converter.isEmpty()) {
                continue;
            }
            Map<String, Object> format = new LinkedHashMap<>();
            format.put("name", plugin.getName());
            format.put("version", plugin.getVersion());
            format.put("extension", converter.get().extension());
            format.put("merge", false);
            format.put("options", converter.get().options().stream().map(DatabaseExportServiceImpl::option).collect(Collectors.toList()));
            formats.add(format);
        }
        return CommonResponse.success(formats);
    }

    private static Map<String, Object> option(ConvertOption option)
    {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", option.getName());
        value.put("labels", option.getLabels());
        value.put("defaultValue", option.getDefaultValue());
        List<Map<String, Object>> choices = new ArrayList<>();
        for (ConvertChoice choice : option.getChoices()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", choice.getValue());
            item.put("labels", choice.getLabels());
            choices.add(item);
        }
        value.put("choices", choices);
        return value;
    }

    // ---- shared ----

    /** A source of the export: its plugin service, a raw-row configure and what its database holds. */
    private final class Target
    {
        private PluginService service;
        private SourceEntity entity;
        private Plugin plugin;
        private ExportStatements statements;
        private String database;
        /** table -> column -> type, in column order, read through the plugin when a table is first needed. */
        private final Map<String, Map<String, String>> columns = new ConcurrentHashMap<>();

        RuntimeConfigure raw()
        {
            return entity.toConfigure(RAW, pluginManager, plugin);
        }

        /** The table's columns and types; null when it does not exist. */
        Map<String, String> columns(String table)
        {
            Map<String, String> found = columns.computeIfAbsent(table, name -> {
                Map<String, String> types = new LinkedHashMap<>();
                SourceColumns.of(service, raw(), database, name).forEach(column -> types.put(column.getName(), column.getType().toUpperCase(Locale.ROOT)));
                return types;
            });
            return found.isEmpty() ? null : found;
        }
    }

    private CommonResponse<Target> target(String code, String database, ExportPlan plan)
    {
        Optional<SourceEntity> entity = repository.findByCode(code);
        if (entity.isEmpty()) {
            return CommonResponse.failure(String.format("Resource [ %s ] not found", code));
        }
        Optional<Plugin> plugin = pluginManager.getPlugin(entity.get().getType());
        if (plugin.isEmpty()) {
            return CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.get().getType()));
        }
        Target target = new Target();
        target.entity = entity.get();
        target.plugin = plugin.get();
        target.service = plugin.get().getService(PluginService.class);
        if (!target.service.managementCapabilities().supports("table:export")) {
            return CommonResponse.failure("Source does not support table:export");
        }
        if (plan != null && plan.includesStructure() && !target.service.managementCapabilities().supports("table:statement")) {
            return CommonResponse.failure("Source does not support table:statement");
        }
        target.statements = new ExportStatements(target.service::sqlIdentifier, target.service::sqlLiteral);
        target.database = database;
        return CommonResponse.success(target);
    }

    private static List<String> values(Object row, int count)
    {
        List<String> values = new ArrayList<>();
        if (row instanceof List) {
            ((List<?>) row).forEach(value -> values.add(value == null ? null : String.valueOf(value)));
        }
        else if (row instanceof JsonNode) {
            ((JsonNode) row).elements().forEachRemaining(value -> values.add(value.isNull() ? null : value.asText()));
        }
        while (values.size() < count) {
            values.add("");
        }
        return values;
    }

    private Optional<ConvertService> converter(ExportPlan plan)
    {
        String format = plan.getFormat() == null ? "" : plan.getFormat();
        return format.isEmpty() || RAW.equals(format) ? Optional.empty() : ConvertFilter.filter(pluginManager, format);
    }

    /** Problems of one table in the plan, empty when it can be exported. */
    private static List<String> problems(Target target, ExportPlan.TablePlan table)
    {
        List<String> problems = new ArrayList<>();
        Map<String, String> columns = target.columns(table.getName());
        if (columns == null) {
            problems.add("Table " + table.getName() + " does not exist");
            return problems;
        }
        for (String field : table.getFields()) {
            if (!columns.containsKey(field)) {
                problems.add("Field " + field + " does not exist");
            }
        }
        for (ExportPlan.Filter filter : table.getFilters()) {
            String operator = filter.getOperator() == null ? "" : filter.getOperator().toUpperCase(Locale.ROOT);
            if (!columns.containsKey(filter.getColumn())) {
                problems.add("Filter field " + filter.getColumn() + " does not exist");
            }
            else if (!ExportStatements.OPERATORS.contains(operator)) {
                problems.add("Unsupported operator " + filter.getOperator());
            }
            else if (!operator.startsWith("IS") && (filter.getValue() == null || filter.getValue().isEmpty())) {
                problems.add("Filter on " + filter.getColumn() + " needs a value");
            }
        }
        return problems;
    }

    private static String rule(ExportPlan.TablePlan table)
    {
        return table.getFilters().isEmpty() ? "all" : "filtered";
    }

    // ---- sample ----

    @Override
    public CommonResponse<Map<String, Object>> sample(String code, String database, ExportPlan plan, String table, int rows)
    {
        Optional<ConvertService> converter = converter(plan);
        CommonResponse<Target> resolved = target(code, database, null);
        if (!Boolean.TRUE.equals(resolved.getStatus())) {
            return CommonResponse.failure(String.valueOf(resolved.getMessage()));
        }
        Target target = resolved.getData();
        ExportPlan.TablePlan chosen = plan.getTables().stream().filter(item -> table.equals(item.getName())).findFirst().orElseGet(() -> {
            ExportPlan.TablePlan item = new ExportPlan.TablePlan();
            item.setName(table);
            return item;
        });
        List<String> problems = problems(target, chosen);
        if (!problems.isEmpty()) {
            return CommonResponse.failure(problems.get(0));
        }
        Response response = target.service.execute(target.raw(), target.statements.select(database, chosen, Math.max(1, Math.min(rows, 20)), 0));
        if (response == null || !Boolean.TRUE.equals(response.getIsSuccessful())) {
            return CommonResponse.failure(response == null ? "Reading the rows failed" : response.getMessage());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("headers", response.getHeaders());
        result.put("rows", response.getColumns());
        if (converter.isPresent()) {
            File directory = null;
            try {
                directory = Files.createTempDirectory("metaforge-export-sample").toFile();
                File file = write(converter.get(), plan, target, database, chosen, response, directory);
                result.put("file", file.getName());
                result.put("content", FileUtils.readFileToString(file, StandardCharsets.UTF_8));
            }
            catch (IOException | RuntimeException error) {
                return CommonResponse.failure(error.getMessage() == null ? "The format could not write the sample" : error.getMessage());
            }
            finally {
                FileUtils.deleteQuietly(directory);
            }
        }
        return CommonResponse.success(result);
    }

    /** Writes rows through the plugin into the directory and returns the file it wrote. */
    private static File write(ConvertService converter, ExportPlan plan, Target target, String database, ExportPlan.TablePlan table, Response rows, File directory)
            throws IOException
    {
        Map<String, String> types = Optional.ofNullable(target.columns(table.getName())).orElse(Map.of());
        ConvertRequest request = new ConvertRequest();
        request.setName(table.getName());
        request.setPath(directory.getAbsolutePath());
        request.setDelimiter("\t");
        request.setHeaders(new ArrayList<>(rows.getHeaders()));
        request.setColumns(rows.getColumns());
        request.setTypes(rows.getHeaders().stream().map(header -> types.getOrDefault(header, "")).collect(Collectors.toList()));
        request.setOptions(plan.getOptions() == null ? Map.of() : plan.getOptions());
        ConvertResponse written = converter.writer(request);
        if (!Boolean.TRUE.equals(written.getSuccessful())) {
            throw new IOException(written.getMessage() == null ? "The format could not write " + table.getName() : written.getMessage());
        }
        File[] files = directory.listFiles(file -> file.getName().startsWith(table.getName() + "."));
        if (files == null || files.length != 1) {
            throw new IOException("The format wrote no file for " + table.getName());
        }
        return files[0];
    }

    // ---- check ----

    @Override
    public CommonResponse<Map<String, Object>> check(String code, String database, ExportPlan plan)
    {
        Optional<ConvertService> converter = converter(plan);
        if (converter.isEmpty()) {
            return CommonResponse.failure("Choose an installed export format");
        }
        if (plan.getTables().isEmpty()) {
            return CommonResponse.failure("Choose at least one table");
        }
        if ("merge".equals(plan.getOrganize())) {
            return CommonResponse.failure("The format " + plan.getFormat() + " can not merge tables into one file");
        }
        CommonResponse<Target> resolved = target(code, database, plan);
        if (!Boolean.TRUE.equals(resolved.getStatus())) {
            return CommonResponse.failure(String.valueOf(resolved.getMessage()));
        }
        Target target = resolved.getData();
        String extension = converter.get().extension();
        List<Map<String, Object>> tables = new ArrayList<>();
        int errors = 0;
        for (ExportPlan.TablePlan table : plan.getTables()) {
            List<String> problems = problems(target, table);
            errors += problems.isEmpty() ? 0 : 1;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", table.getName());
            row.put("structure", plan.includesStructure());
            row.put("data", true);
            row.put("rule", rule(table));
            row.put("file", ExportStatements.fileName(table.getName(), extension));
            row.put("status", problems.isEmpty() ? "pass" : "error");
            row.put("messages", problems);
            tables.add(row);
        }
        List<String> files = tables.stream().map(row -> String.valueOf(row.get("file"))).collect(Collectors.toList());
        if (plan.includesStructure()) {
            files.add(SCHEMA);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tables", tables);
        result.put("files", files);
        result.put("output", output(plan, files.size(), extension, plan.getTables().get(0).getName()));
        result.put("errors", errors);
        return CommonResponse.success(result);
    }

    /** One file without compression is downloaded as it is; otherwise everything goes into a ZIP. */
    private static String output(ExportPlan plan, int files, String extension, String table)
    {
        String base = ExportStatements.safeName(plan.getFileName(), "export");
        return files == 1 && !plan.isCompress() ? ExportStatements.fileName(ExportStatements.safeName(table, base), extension) : base + ".zip";
    }

    // ---- execute ----

    /** A running or finished export; tables keep their progress, the file is written when all are done. */
    private static final class Job
    {
        private final String id = UUID.randomUUID().toString();
        private final String user;
        private final long created = System.currentTimeMillis();
        private final boolean keep;
        private final Map<String, Map<String, Object>> tables = new LinkedHashMap<>();
        private final AtomicBoolean stopped = new AtomicBoolean();
        private volatile String status = "running";
        private volatile String message;
        private volatile File file;
        private volatile String name;
        private volatile File directory;

        private Job(String user, boolean keep)
        {
            this.user = user;
            this.keep = keep;
        }
    }

    @Override
    public CommonResponse<Map<String, Object>> execute(String code, String database, ExportPlan plan)
    {
        CommonResponse<Map<String, Object>> checked = check(code, database, plan);
        if (!Boolean.TRUE.equals(checked.getStatus())) {
            return checked;
        }
        if (((Number) checked.getData().get("errors")).intValue() > 0) {
            return CommonResponse.failure("Fix the tables that do not pass the check first");
        }
        ConvertService converter = converter(plan).orElseThrow();
        Target target = target(code, database, plan).getData();
        expire();
        Job job = new Job(currentUser(), plan.isKeepRecord());
        for (ExportPlan.TablePlan table : plan.getTables()) {
            Map<String, Object> item = new ConcurrentHashMap<>();
            item.put("name", table.getName());
            item.put("status", "waiting");
            item.put("written", 0L);
            item.put("total", -1L);
            job.tables.put(table.getName(), item);
        }
        jobs.put(job.id, job);
        String output = String.valueOf(checked.getData().get("output"));
        runner.submit(() -> run(job, target, converter, database, plan, output));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", job.id);
        return CommonResponse.success(result);
    }

    private void run(Job job, Target target, ConvertService converter, String database, ExportPlan plan, String output)
    {
        Map<String, String> schemas = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(4, plan.getConcurrency())));
        try {
            job.directory = Files.createTempDirectory("metaforge-export").toFile();
            for (ExportPlan.TablePlan table : plan.getTables()) {
                pool.submit(() -> exportTable(job, target, converter, database, plan, table, schemas));
            }
            pool.shutdown();
            if (!pool.awaitTermination(6, TimeUnit.HOURS)) {
                pool.shutdownNow();
                job.stopped.set(true);
                job.message = "The export took longer than six hours";
            }
            if (job.stopped.get()) {
                job.status = "failed";
                job.message = job.message == null ? "The export stopped at a failing table" : job.message;
                return;
            }
            if (plan.includesStructure()) {
                String schema = plan.getTables().stream().map(table -> schemas.getOrDefault(table.getName(), "")).collect(Collectors.joining("\n"));
                FileUtils.writeStringToFile(new File(job.directory, SCHEMA), schema, StandardCharsets.UTF_8);
            }
            job.file = pack(job.directory, plan, output);
            job.name = job.file.getParentFile().equals(job.directory) ? job.file.getName() : output;
            boolean failed = job.tables.values().stream().anyMatch(item -> "failed".equals(item.get("status")));
            job.status = failed ? "partial" : "done";
        }
        catch (IOException | RuntimeException error) {
            log.warn("Export {} failed", job.id, error);
            job.status = "failed";
            job.message = error.getMessage();
        }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            job.status = "failed";
            job.message = "The export was interrupted";
        }
        finally {
            pool.shutdownNow();
        }
    }

    private void exportTable(Job job, Target target, ConvertService converter, String database, ExportPlan plan, ExportPlan.TablePlan table, Map<String, String> schemas)
    {
        Map<String, Object> item = job.tables.get(table.getName());
        if (job.stopped.get()) {
            item.put("status", "cancelled");
            return;
        }
        item.put("status", "running");
        RuntimeConfigure configure = target.raw();
        try {
            if (plan.includesStructure()) {
                // The plugin builds the CREATE statement, as the DDL tab shows it.
                Response create = target.service.getTableDdl(configure, database, table.getName(), "create");
                if (create == null || !Boolean.TRUE.equals(create.getIsSuccessful()) || create.getContent() == null) {
                    throw new IOException(create == null ? "Reading the structure failed" : create.getMessage());
                }
                String statement = create.getContent();
                schemas.put(table.getName(), target.statements.schema(database, table.getName(), statement, plan.getStructure()));
            }
            Response total = target.service.execute(configure, target.statements.count(database, table));
            if (total != null && Boolean.TRUE.equals(total.getIsSuccessful()) && !total.getColumns().isEmpty()) {
                item.put("total", parseLong(values(total.getColumns().get(0), 1).get(0)));
            }
            Response rows = read(job, target, configure, database, plan, table, item);
            write(converter, plan, target, database, table, rows, job.directory);
            item.put("status", "done");
        }
        catch (IOException | RuntimeException error) {
            log.warn("Export of {}.{} failed", database, table.getName(), error);
            item.put("status", "failed");
            item.put("message", error.getMessage() == null ? "Export failed" : error.getMessage());
            if ("stop".equals(plan.getFailure())) {
                job.stopped.set(true);
                job.message = table.getName() + ": " + item.get("message");
            }
        }
    }

    /** Every row of a table, read in pages of batchSize (or in one query), with the written count kept current. */
    private static Response read(Job job, Target target, RuntimeConfigure configure, String database, ExportPlan plan, ExportPlan.TablePlan table, Map<String, Object> item)
            throws IOException
    {
        long batch = "single".equals(plan.getReading()) ? 0 : Math.max(1, Math.min(100_000, plan.getBatchSize()));
        Response all = null;
        List<Object> rows = new ArrayList<>();
        for (long offset = 0; ; offset += batch) {
            if (job.stopped.get()) {
                throw new IOException("Stopped");
            }
            Response page = target.service.execute(configure, target.statements.select(database, table, batch, offset));
            if (page == null || !Boolean.TRUE.equals(page.getIsSuccessful())) {
                throw new IOException(page == null ? "Reading the rows failed" : page.getMessage());
            }
            all = all == null ? page : all;
            rows.addAll(page.getColumns());
            item.put("written", (long) rows.size());
            if (batch == 0 || page.getColumns().size() < batch) {
                break;
            }
        }
        all.setColumns(rows);
        return all;
    }

    private static File pack(File directory, ExportPlan plan, String output)
            throws IOException
    {
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            throw new IOException("The export wrote no file");
        }
        Arrays.sort(files);
        if (!output.endsWith(".zip") && files.length == 1) {
            return files[0];
        }
        File zip = new File(directory.getParentFile(), directory.getName() + ".zip");
        try (OutputStream out = Files.newOutputStream(zip.toPath()); ZipOutputStream stream = new ZipOutputStream(out)) {
            stream.setLevel(plan.isCompress() ? Deflater.DEFAULT_COMPRESSION : Deflater.NO_COMPRESSION);
            for (File file : files) {
                stream.putNextEntry(new ZipEntry(file.getName()));
                Files.copy(file.toPath(), stream);
                stream.closeEntry();
            }
        }
        return zip;
    }

    private static long parseLong(String value)
    {
        if (value == null) {
            return -1;
        }
        try {
            return Long.parseLong(value.trim());
        }
        catch (NumberFormatException error) {
            return -1;
        }
    }

    // ---- job ----

    private Job owned(String id)
    {
        Job job = jobs.get(id);
        return job == null || !job.user.equals(currentUser()) ? null : job;
    }

    @Override
    public CommonResponse<Map<String, Object>> job(String id)
    {
        Job job = owned(id);
        if (job == null) {
            return CommonResponse.failure("Export job not found");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", job.status);
        result.put("message", job.message);
        result.put("tables", new ArrayList<>(job.tables.values()));
        File file = job.file;
        if (file != null) {
            result.put("file", job.name);
            result.put("size", file.length());
        }
        return CommonResponse.success(result);
    }

    @Override
    public CommonResponse<TableExportService.ExportFile> file(String id)
    {
        Job job = owned(id);
        if (job == null || job.file == null) {
            return CommonResponse.failure("The export file is not ready");
        }
        try {
            TableExportService.ExportFile file = new TableExportService.ExportFile(job.name, FileUtils.readFileToByteArray(job.file));
            if (!job.keep) {
                remove(job);
            }
            return CommonResponse.success(file);
        }
        catch (IOException error) {
            return CommonResponse.failure(error.getMessage());
        }
    }

    /** Drops finished jobs older than two hours with their files. */
    private void expire()
    {
        long now = System.currentTimeMillis();
        jobs.values().stream().filter(job -> !"running".equals(job.status) && now - job.created > KEEP).collect(Collectors.toList()).forEach(this::remove);
    }

    private void remove(Job job)
    {
        jobs.remove(job.id);
        if (job.file != null && job.directory != null && !job.file.getParentFile().equals(job.directory)) {
            FileUtils.deleteQuietly(job.file);
        }
        FileUtils.deleteQuietly(job.directory);
    }
}
