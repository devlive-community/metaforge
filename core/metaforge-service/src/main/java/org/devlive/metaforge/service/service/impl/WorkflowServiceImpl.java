package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.CodeUtils;
import org.devlive.metaforge.executor.ExecutorService;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.configure.ExecutorConfigure;
import org.devlive.metaforge.executor.configure.ExecutorRequest;
import org.devlive.metaforge.executor.configure.ExecutorResponse;
import org.devlive.metaforge.executor.workflow.WorkflowPlan;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.WorkflowEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.itransient.configuration.Configuration;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.repository.WorkflowRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.devlive.metaforge.service.service.WorkflowService;
import org.devlive.metaforge.service.workflow.WorkflowCatalog;
import org.devlive.metaforge.service.workflow.WorkflowExecutorDefinition;
import org.devlive.metaforge.service.workflow.WorkflowRun;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.adapter.RowCallback;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class WorkflowServiceImpl
        implements WorkflowService
{
    /** Rows returned by a query preview in the editor. */
    private static final int PREVIEW_ROWS = 50;
    private final InitializerConfigure initializer;
    private final PluginManager plugins;
    private final WorkflowRepository repository;
    private final SourceRepository sources;
    private final RuntimeConfigureService runtime;
    private final WorkflowCatalog catalog;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, ActiveRun> active = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), runnable -> {
                Thread thread = new Thread(runnable, "workflow-worker");
                thread.setDaemon(true);
                return thread;
            });

    public WorkflowServiceImpl(InitializerConfigure initializer, PluginManager plugins, WorkflowRepository repository,
            SourceRepository sources, RuntimeConfigureService runtime, WorkflowCatalog catalog)
    {
        this.initializer = initializer;
        this.plugins = plugins;
        this.repository = repository;
        this.sources = sources;
        this.runtime = runtime;
        this.catalog = catalog;
    }

    @Override
    public synchronized CommonResponse<WorkflowEntity> saveOrUpdate(BaseRepository<WorkflowEntity, Long> ignored, WorkflowEntity form)
    {
        UserEntity user = UserDetailsService.getUser();
        WorkflowEntity entity = new WorkflowEntity();
        if (form.getCode() != null) {
            Optional<WorkflowEntity> previous = repository.findByCode(form.getCode());
            if (previous.isPresent()) {
                if (!owned(previous.get(), user)) {
                    return CommonResponse.failure("Workflow not found");
                }
                entity = previous.get();
            }
            else if (form.getId() != null) {
                return CommonResponse.failure("Workflow not found");
            }
        }
        if (active.containsKey(form.getCode() == null ? "" : form.getCode()) || entity.getState() == RunState.RUNNING) {
            return CommonResponse.failure("Stop the workflow before editing it");
        }
        if (form.getName() == null || form.getName().isBlank() || form.getName().length() > 200) {
            return CommonResponse.failure("Workflow name is required and must not exceed 200 characters");
        }
        try {
            catalog.executor(form.getExecutor());
            entity.setCode(form.getCode() == null ? CodeUtils.generateCode(false) : form.getCode());
            entity.setName(form.getName().trim());
            entity.setExecutor(form.getExecutor());
            if (form.getConfigure() != null) {
                Integer revision = Optional.ofNullable(entity.getConfigure()).map(Configuration::getRevision).orElse(0);
                form.getConfigure().setRevision(revision);
                form.getConfigure().setPublishedAt(null);
            }
            entity.setConfigure(form.getConfigure());
            entity.setUserId(user.getId());
            entity.setWork(workHome(user, entity));
            if (entity.getState() == null) {
                entity.setState(RunState.CREATED);
            }
            return CommonResponse.success(repository.save(entity));
        }
        catch (IllegalArgumentException e) {
            return CommonResponse.failure(e.getMessage());
        }
    }

    @Override
    public CommonResponse<WorkflowEntity> getByCode(BaseRepository<WorkflowEntity, Long> ignored, String code)
    {
        return own(code).map(CommonResponse::success).orElseGet(() -> CommonResponse.failure("Workflow not found"));
    }

    @Override
    public CommonResponse<WorkflowEntity> getById(BaseRepository<WorkflowEntity, Long> ignored, Long id)
    {
        return repository.findById(id).filter(entity -> owned(entity, UserDetailsService.getUser()))
                .map(CommonResponse::success).orElseGet(() -> CommonResponse.failure("Workflow not found"));
    }

    @Override
    public CommonResponse<List<String>> validate(WorkflowEntity workflow)
    {
        return CommonResponse.success(catalog.validate(workflow));
    }

    @Override
    public synchronized CommonResponse<WorkflowEntity> publish(String code)
    {
        WorkflowEntity entity = own(code).orElse(null);
        if (entity == null) {
            return CommonResponse.failure("Workflow not found");
        }
        if (active.containsKey(code)) {
            return CommonResponse.failure("Stop the workflow before publishing");
        }
        List<String> errors = catalog.validate(entity);
        if (!errors.isEmpty()) {
            return CommonResponse.failure(String.join("\n", errors));
        }
        entity.getConfigure().setPublishedAt(System.currentTimeMillis());
        entity.getConfigure().setRevision(Optional.ofNullable(entity.getConfigure().getRevision()).orElse(0) + 1);
        return CommonResponse.success(repository.save(entity));
    }

    @Override
    public CommonResponse<Map<String, Object>> catalog()
    {
        try {
            return CommonResponse.success(catalog.catalog());
        }
        catch (IllegalArgumentException e) {
            return CommonResponse.failure(e.getMessage());
        }
    }

    @Override
    public synchronized CommonResponse<String> restart(String code)
    {
        WorkflowEntity entity = own(code).orElse(null);
        if (entity == null) {
            return CommonResponse.failure("Workflow not found");
        }
        if (active.containsKey(code) || entity.getState() == RunState.RUNNING) {
            return CommonResponse.failure("Workflow is already running");
        }
        List<String> errors = catalog.validate(entity);
        if (!errors.isEmpty()) {
            return CommonResponse.failure(String.join("\n", errors));
        }
        try {
            UserEntity user = UserDetailsService.getUser();
            WorkflowCatalog.WorkflowExecutor executor = catalog.executor(entity.getExecutor());
            Plugin plugin = executor.plugin();
            WorkflowRun run = new WorkflowRun();
            run.setId(UUID.randomUUID().toString());
            run.setCode(code);
            run.setName(entity.getName());
            run.setExecutor(entity.getExecutor());
            run.setState(RunState.RUNNING);
            run.setStartedAt(System.currentTimeMillis());
            run.setNodes(entity.getConfigure().getNodes().stream().map(node -> Map.of(
                    "id", node.getId(), "label", node.getLabel() == null ? node.getTid() : node.getLabel(),
                    "category", node.getCategory())).collect(Collectors.toList()));
            entity.setWork(workHome(user, entity));
            Map<String, String> options = new HashMap<>(runtime.getEffective(RuntimeConfigureService.CATEGORY_EXECUTOR, plugin.getName(), plugin.runtimeConfigures()));
            WorkflowPlan plan = catalog.plan(entity, run.getId(), user.getUsername(), options, sourceCode -> source(sourceCode, user));
            ExecutorRequest request = executor.service().compile(plan);
            request.setPluginManager(plugins);
            request.setMetricsListener(metrics -> run.setMetrics(metrics));
            Files.createDirectories(Path.of(entity.getWork()));
            writeRun(entity, run);
            entity.setState(RunState.RUNNING);
            entity.setMessage(null);
            entity.setElapsed(null);
            repository.save(entity);
            ActiveRun handle = new ActiveRun(run, request, executor.service(), executor.definition().getCapabilities());
            active.put(code, handle);
            try {
                handle.future = workers.submit(() -> execute(entity, handle));
            }
            catch (RuntimeException e) {
                active.remove(code);
                finish(entity, handle, RunState.FAILURE, e.getMessage());
                throw e;
            }
            return CommonResponse.success(run.getId());
        }
        catch (IOException | RuntimeException e) {
            log.warn("Cannot start workflow {}", code, e);
            return CommonResponse.failure(e.getMessage());
        }
    }

    private void execute(WorkflowEntity entity, ActiveRun handle)
    {
        try {
            synchronized (this) {
                if (handle.run.getState() != RunState.RUNNING) {
                    return;
                }
                handle.started = true;
            }
            ExecutorResponse response = handle.service.start(handle.request);
            synchronized (this) {
                handle.run.setMetrics(response.getMetrics());
                // Executors that do not report row counts leave the count unknown rather than zero.
                boolean hasCount = handle.capabilities.isRowCount() || response.getMetrics() != null || response.getCount() > 0;
                handle.run.setCount(hasCount ? response.getCount() : null);
                handle.run.setPreview(response.getPreview());
                if (handle.run.getState() == RunState.RUNNING) {
                    finish(entity, handle, response.getState(), response.getMessage());
                }
                else {
                    writeRun(entity, handle.run);
                }
            }
        }
        catch (Exception e) {
            synchronized (this) {
                if (handle.run.getState() == RunState.RUNNING) {
                    finish(entity, handle, RunState.FAILURE, e.getMessage());
                }
            }
            log.warn("Workflow {} failed", entity.getCode(), e);
        }
        finally {
            active.remove(entity.getCode(), handle);
        }
    }

    private void finish(WorkflowEntity entity, ActiveRun handle, RunState state, String message)
    {
        handle.run.setMessage(message);
        handle.run.setFinishedAt(System.currentTimeMillis());
        handle.run.setElapsed(handle.run.getFinishedAt() - handle.run.getStartedAt());
        handle.run.setState(state == null ? RunState.FAILURE : state);
        entity.setState(handle.run.getState());
        entity.setMessage(message);
        entity.setElapsed(handle.run.getElapsed());
        entity.setUpdateTime(new Date());
        repository.save(entity);
        writeRun(entity, handle.run);
    }

    @Override
    public synchronized CommonResponse<Boolean> stop(String code)
    {
        WorkflowEntity entity = own(code).orElse(null);
        if (entity == null) {
            return CommonResponse.failure("Workflow not found");
        }
        ActiveRun handle = active.get(code);
        if (handle == null) {
            return CommonResponse.failure("Workflow is not running on this server");
        }
        if (!handle.capabilities.stoppable()) {
            return CommonResponse.failure("This executor cannot stop a running workflow");
        }
        try {
            boolean cooperative = handle.started && handle.capabilities.cooperativeStop();
            if (cooperative) {
                ExecutorResponse response = handle.service.stop(handle.request);
                if (!response.getSuccessful()) {
                    return CommonResponse.failure(response.getMessage());
                }
            }
            if (handle.future != null && !cooperative) {
                handle.future.cancel(true);
            }
            finish(entity, handle, RunState.STOPPED, "Stopped by user");
            // Keep the handle registered until the interrupted execution has actually exited.
            if (!handle.started) {
                active.remove(code, handle);
            }
            return CommonResponse.success(true);
        }
        catch (Exception e) {
            return CommonResponse.failure(e.getMessage());
        }
    }

    @Override
    public CommonResponse<List<Map<String, String>>> queryColumns(String source, String query)
    {
        CommonResponse<Map<String, Object>> read = readQuery(source, query, 0);
        if (!Boolean.TRUE.equals(read.getStatus())) {
            return CommonResponse.failure(String.valueOf(read.getMessage()));
        }
        @SuppressWarnings("unchecked")
        List<Map<String, String>> columns = (List<Map<String, String>>) read.getData().get("columns");
        return CommonResponse.success(columns);
    }

    @Override
    public CommonResponse<Map<String, Object>> queryPreview(String source, String query)
    {
        return readQuery(source, query, PREVIEW_ROWS);
    }

    /**
     * Reads the columns and at most {@code limit} rows of a query through the data source plugin's
     * streaming read, stopping as soon as enough is known; the query itself is never rewritten.
     */
    private CommonResponse<Map<String, Object>> readQuery(String source, String query, int limit)
    {
        String statement = query == null ? "" : query.strip().replaceAll("[;\\s]+$", "");
        if (source == null || source.isBlank() || statement.isEmpty()) {
            return CommonResponse.failure("Select a data source and enter a query");
        }
        try {
            ExecutorConfigure configure = source(source, UserDetailsService.getUser());
            PluginService plugin = configure.getPlugin();
            if (plugin == null || !plugin.supportsStreaming()) {
                return CommonResponse.failure("The data source cannot describe query columns");
            }
            List<Map<String, String>> columns = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();
            List<String> names = new ArrayList<>();
            long started = System.currentTimeMillis();
            try {
                plugin.executeStream(configure.getOriginConfigure(), statement, Math.max(1, limit), new RowCallback()
                {
                    @Override
                    public void onStatement(java.sql.Statement jdbc)
                    {
                        try {
                            jdbc.setMaxRows(Math.max(1, limit));
                        }
                        catch (java.sql.SQLException ignored) {
                            // Not every driver supports a row limit; reading stops after the limit either way.
                        }
                    }

                    @Override
                    public void onSchema(List<String> headers, List<String> types)
                    {
                        for (int index = 0; index < headers.size(); index++) {
                            Map<String, String> column = new LinkedHashMap<>();
                            column.put("name", headers.get(index));
                            column.put("type", types != null && index < types.size() && types.get(index) != null ? types.get(index) : "");
                            columns.add(column);
                            names.add(headers.get(index));
                        }
                        if (limit <= 0) {
                            throw new SchemaRead();
                        }
                    }

                    @Override
                    public void onRow(List<Object> row)
                    {
                        if (rows.size() >= limit) {
                            throw new SchemaRead();
                        }
                        Map<String, Object> values = new LinkedHashMap<>();
                        for (int index = 0; index < names.size(); index++) {
                            Object value = index < row.size() ? row.get(index) : null;
                            // Driver types such as timestamps or blobs are shown as text.
                            values.put(names.get(index), value == null || value instanceof Number || value instanceof Boolean ? value : abbreviate(value.toString()));
                        }
                        rows.add(values);
                        if (rows.size() >= limit) {
                            throw new SchemaRead();
                        }
                    }
                });
            }
            catch (RuntimeException stopped) {
                // Expected once enough is read; plugins may wrap the stop in their own exception.
                if (!(stopped instanceof SchemaRead) && columns.isEmpty()) {
                    throw stopped;
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("elapsed", System.currentTimeMillis() - started);
            return CommonResponse.success(result);
        }
        catch (RuntimeException e) {
            log.warn("Cannot read a workflow query", e);
            return CommonResponse.failure(e.getMessage());
        }
    }

    private static String abbreviate(String value)
    {
        return value.length() > 1000 ? value.substring(0, 1000) + "…" : value;
    }

    /** Stops a schema-only read once the plugin has reported the columns. */
    private static final class SchemaRead
            extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        private SchemaRead()
        {
            super(null, null, false, false);
        }
    }

    /** A saved data source the user owns or that is published, with its plugin and connection configuration. */
    private ExecutorConfigure source(String code, UserEntity user)
    {
        SourceEntity source = sources.findByCode(code)
                .filter(value -> Objects.equals(value.getUserId(), user.getId()) || Boolean.TRUE.equals(value.getPublish()))
                .orElseThrow(() -> new IllegalArgumentException("Data source is unavailable"));
        Plugin plugin = plugins.getPlugin(source.getType()).orElseThrow(() -> new IllegalArgumentException("Data source plugin is unavailable"));
        ExecutorConfigure configure = new ExecutorConfigure(source.getType());
        configure.setPlugin(plugin.getService(PluginService.class));
        RuntimeConfigure origin = source.toConfigure("NoneConvert", plugins, plugin);
        origin.setPluginManager(plugins);
        configure.setOriginConfigure(origin);
        return configure;
    }

    @Override
    public CommonResponse<List<WorkflowRun>> runs(String code)
    {
        WorkflowEntity entity = own(code).orElse(null);
        if (entity == null) {
            return CommonResponse.failure("Workflow not found");
        }
        Map<String, WorkflowRun> result = new HashMap<>();
        if (entity.getWork() != null && Files.isDirectory(Path.of(entity.getWork()))) {
            try (Stream<Path> files = Files.list(Path.of(entity.getWork()))) {
                files.filter(file -> file.getFileName().toString().matches("run-[a-f0-9-]{36}\\.json"))
                        .sorted(Comparator.comparingLong((Path path) -> path.toFile().lastModified()).reversed())
                        .limit(100)
                        .forEach(file -> {
                            try {
                                WorkflowRun run = mapper.readValue(file.toFile(), WorkflowRun.class);
                                if (run.getState() == RunState.RUNNING && !active.containsKey(code)) {
                                    run.setState(RunState.INTERRUPTED);
                                    run.setMessage("Execution interrupted by server restart");
                                }
                                result.put(run.getId(), run);
                            }
                            catch (IOException e) {
                                log.warn("Cannot read workflow run {}", file, e);
                            }
                        });
            }
            catch (IOException e) {
                return CommonResponse.failure("Cannot read execution records");
            }
        }
        ActiveRun running = active.get(code);
        if (running != null) {
            result.put(running.run.getId(), running.run);
        }
        return CommonResponse.success(result.values().stream().sorted(Comparator.comparingLong(WorkflowRun::getStartedAt).reversed()).limit(100).map(this::summary).collect(Collectors.toList()));
    }

    private WorkflowRun summary(WorkflowRun run)
    {
        WorkflowRun summary = new WorkflowRun();
        summary.setId(run.getId());
        summary.setCode(run.getCode());
        summary.setName(run.getName());
        summary.setExecutor(run.getExecutor());
        summary.setStartedAt(run.getStartedAt());
        summary.setFinishedAt(run.getFinishedAt());
        summary.setElapsed(run.getElapsed());
        summary.setState(run.getState());
        summary.setCount(run.getCount());
        return summary;
    }

    @Override
    public CommonResponse<WorkflowRun> run(String code, String runId)
    {
        WorkflowEntity entity = own(code).orElse(null);
        if (entity == null || runId == null || !runId.matches("[a-f0-9-]{36}")) {
            return CommonResponse.failure("Execution not found");
        }
        ActiveRun running = active.get(code);
        if (running != null && running.run.getId().equals(runId)) {
            return CommonResponse.success(running.run);
        }
        if (entity.getWork() != null) {
            Path path = Path.of(entity.getWork(), "run-" + runId + ".json");
            try {
                WorkflowRun run = mapper.readValue(path.toFile(), WorkflowRun.class);
                if (run.getState() == RunState.RUNNING) {
                    run.setState(RunState.INTERRUPTED);
                    run.setMessage("Execution interrupted by server restart");
                }
                return CommonResponse.success(run);
            }
            catch (IOException e) {
                return CommonResponse.failure("Execution not found");
            }
        }
        return CommonResponse.failure("Execution not found");
    }

    @Override
    public CommonResponse<List<String>> log(String code)
    {
        return log(code, null);
    }

    @Override
    public CommonResponse<List<String>> log(String code, String runId)
    {
        WorkflowEntity entity = own(code).orElse(null);
        if (entity == null) {
            return CommonResponse.failure("Workflow not found");
        }
        if (runId != null && !runId.matches("[a-f0-9-]{36}")) {
            return CommonResponse.failure("Invalid run identifier");
        }
        if (runId == null) {
            List<WorkflowRun> records = runs(code).getData();
            runId = records == null || records.isEmpty() ? code : records.get(0).getId();
        }
        if (entity.getWork() == null || !Files.exists(Path.of(entity.getWork(), runId + ".log"))) {
            return CommonResponse.success(List.of());
        }
        Path path = Path.of(entity.getWork(), runId + ".log");
        try (var file = new java.io.RandomAccessFile(path.toFile(), "r")) {
            // Read only the log tail; polling a long-running job must not load an unbounded file.
            long offset = Math.max(0, file.length() - 262144);
            file.seek(offset);
            byte[] bytes = new byte[(int) (file.length() - offset)];
            file.readFully(bytes);
            List<String> lines = new ArrayList<>(List.of(new String(bytes, StandardCharsets.UTF_8).split("\n")));
            if (offset > 0 && !lines.isEmpty()) {
                lines.remove(0);
            }
            return CommonResponse.success(lines);
        }
        catch (IOException e) {
            return CommonResponse.failure("Cannot read execution log");
        }
    }

    @Override
    public CommonResponse<Map<String, Long>> statistics()
    {
        List<WorkflowEntity> rows = repository.findAllByUserId(UserDetailsService.getUser().getId());
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("total", (long) rows.size());
        counts.put("running", rows.stream().filter(row -> row.getState() == RunState.RUNNING).count());
        counts.put("success", rows.stream().filter(row -> row.getState() == RunState.SUCCESS).count());
        counts.put("failure", rows.stream().filter(row -> row.getState() == RunState.FAILURE || row.getState() == RunState.TIMEOUT || row.getState() == RunState.INTERRUPTED).count());
        return CommonResponse.success(counts);
    }

    @Override
    public synchronized CommonResponse<String> deleteByCode(BaseRepository<WorkflowEntity, Long> ignored, String code)
    {
        WorkflowEntity entity = own(code).orElse(null);
        if (entity == null) {
            return CommonResponse.failure("Workflow not found");
        }
        if (active.containsKey(code) || entity.getState() == RunState.RUNNING) {
            return CommonResponse.failure("Stop the workflow before deleting it");
        }
        repository.delete(entity);
        if (entity.getWork() != null) {
            try {
                FileUtils.deleteDirectory(Path.of(entity.getWork()).toFile());
            }
            catch (IOException e) {
                log.warn("Cannot delete workflow work directory", e);
            }
        }
        return CommonResponse.success(code);
    }

    @Override
    public CommonResponse<Long> deleteById(BaseRepository<WorkflowEntity, Long> ignored, Long id)
    {
        return repository.findById(id).filter(entity -> owned(entity, UserDetailsService.getUser()))
                .map(entity -> {
                    CommonResponse<String> response = deleteByCode(repository, entity.getCode());
                    return Boolean.TRUE.equals(response.getStatus()) ? CommonResponse.success(id) : CommonResponse.<Long>failure(String.valueOf(response.getMessage()));
                }).orElseGet(() -> CommonResponse.failure("Workflow not found"));
    }

    private Optional<WorkflowEntity> own(String code)
    {
        UserEntity user = UserDetailsService.getUser();
        return repository.findByCode(code).filter(entity -> owned(entity, user));
    }

    private boolean owned(WorkflowEntity entity, UserEntity user)
    {
        return user != null && user.getId() != null && Objects.equals(entity.getUserId(), user.getId());
    }

    private String workHome(UserEntity user, WorkflowEntity entity)
    {
        return entity.getWork() != null ? entity.getWork() : Path.of(initializer.getDataHome(), user.getUsername(), "workflow", entity.getCode()).toString();
    }

    private void writeRun(WorkflowEntity entity, WorkflowRun run)
    {
        try {
            Path folder = Path.of(entity.getWork());
            Files.createDirectories(folder);
            Path temporary = Files.createTempFile(folder, ".run-", ".tmp");
            try {
                mapper.writeValue(temporary.toFile(), run);
                Files.move(temporary, folder.resolve("run-" + run.getId() + ".json"), StandardCopyOption.REPLACE_EXISTING);
            }
            finally {
                Files.deleteIfExists(temporary);
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Cannot store execution record", e);
        }
    }

    @PreDestroy
    public synchronized void close()
    {
        for (ActiveRun handle : active.values()) {
            if (handle.started && handle.capabilities.cooperativeStop()) {
                handle.service.stop(handle.request);
            }
        }
        workers.shutdownNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterrupted()
    {
        try {
            repository.findAllByState(RunState.RUNNING).forEach(entity -> {
                entity.setState(RunState.INTERRUPTED);
                entity.setMessage("Execution interrupted by server restart");
                repository.save(entity);
            });
        }
        catch (org.springframework.dao.DataAccessException ex) {
            // 未安装的空库还没有表，安装向导完成前没有可恢复的运行；不能因此让服务退出
            // A database that is not installed yet has no tables and nothing to recover; the server must still start.
            log.warn("Workflow recovery skipped: {}", ex.getMostSpecificCause().getMessage());
        }
    }

    private static class ActiveRun
    {
        private final WorkflowRun run;
        private final ExecutorRequest request;
        private final ExecutorService service;
        private final WorkflowExecutorDefinition.Capabilities capabilities;
        private Future<?> future;
        private boolean started;

        private ActiveRun(WorkflowRun run, ExecutorRequest request, ExecutorService service, WorkflowExecutorDefinition.Capabilities capabilities)
        {
            this.run = run;
            this.request = request;
            this.service = service;
            this.capabilities = capabilities;
        }
    }
}
