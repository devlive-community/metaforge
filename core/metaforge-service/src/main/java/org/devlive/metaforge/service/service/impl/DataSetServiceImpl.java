package org.devlive.metaforge.service.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.devlive.metaforge.common.enums.DataSetState;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.sql.SqlBuilder;
import org.devlive.metaforge.common.sql.configure.SqlBody;
import org.devlive.metaforge.common.sql.configure.SqlColumn;
import org.devlive.metaforge.common.sql.configure.SqlOperator;
import org.devlive.metaforge.common.sql.configure.SqlOrder;
import org.devlive.metaforge.common.sql.configure.SqlType;
import org.devlive.metaforge.executor.ExecutorService;
import org.devlive.metaforge.executor.common.RunProtocol;
import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.executor.common.RunStateMachine;
import org.devlive.metaforge.executor.configure.ExecutorConfigure;
import org.devlive.metaforge.executor.configure.ExecutorRequest;
import org.devlive.metaforge.executor.configure.ExecutorResponse;
import org.devlive.metaforge.executor.configure.OriginColumn;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.plugin.configure.PluginConfigurePipelineType;
import org.devlive.metaforge.scheduler.SchedulerRequest;
import org.devlive.metaforge.scheduler.SchedulerService;
import org.devlive.metaforge.service.adapter.PageRequestAdapter;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.PipelineFieldBody;
import org.devlive.metaforge.service.body.adhoc.Adhoc;
import org.devlive.metaforge.service.common.ConfigureUtils;
import org.devlive.metaforge.service.common.FolderUtils;
import org.devlive.metaforge.service.dataset.DatasetVirtualColumns;
import org.devlive.metaforge.service.dataset.sync.SyncJob;
import org.devlive.metaforge.service.dataset.sync.SyncJobManager;
import org.devlive.metaforge.service.dataset.sync.SyncMetricsView;
import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.ColumnMode;
import org.devlive.metaforge.service.enums.ColumnType;
import org.devlive.metaforge.service.enums.CreatedMode;
import org.devlive.metaforge.service.enums.EntityType;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.enums.QueryMode;
import org.devlive.metaforge.service.enums.SyncMode;
import org.devlive.metaforge.service.event.NotificationEventPublisher;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.initializer.job.DatasetJob;
import org.devlive.metaforge.service.model.CreatedModel;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.DataSetColumnRepository;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.repository.DatasetHistoryRepository;
import org.devlive.metaforge.service.repository.NotificationRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.DataSetService;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.PluginType;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.sql.EngineType;
import org.devlive.metaforge.sql.builder.ColumnBuilder;
import org.devlive.metaforge.sql.builder.TableBuilder;
import org.devlive.metaforge.sql.model.Column;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Service
@Slf4j
@SuppressFBWarnings(value = {"REC_CATCH_EXCEPTION", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "EI_EXPOSE_REP2", "DLS_DEAD_LOCAL_STORE"})
public class DataSetServiceImpl
        implements DataSetService
{
    // 同步期间每 SIZE_REFRESH_EVERY 次指标落库才刷一次 totalSize（查 ClickHouse system.parts 较重）
    private static final int SIZE_REFRESH_EVERY = 10;

    // 同步完成后刷新表元数据（行数、大小）的服务端阶段 / service-side stage that refreshes table metadata after a sync
    private static final String SYNC_STAGE_FINALIZE = "FINALIZE";

    public final DataSetColumnRepository columnRepository;
    private final DataSetRepository repository;
    private final DatasetHistoryRepository historyRepository;
    private final PluginManager pluginManager;
    private final InitializerConfigure initializerConfigure;
    private final org.quartz.Scheduler scheduler;
    private final Environment environment;
    private final SourceRepository sourceRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher eventPublisher;
    private final RuntimeConfigureService runtimeConfigureService;
    private final SyncJobManager syncJobManager;

    public DataSetServiceImpl(DataSetRepository repository, DataSetColumnRepository columnRepository, DatasetHistoryRepository historyRepository, PluginManager pluginManager, InitializerConfigure initializerConfigure, org.quartz.Scheduler scheduler, Environment environment, SourceRepository sourceRepository, NotificationRepository notificationRepository, NotificationEventPublisher eventPublisher, RuntimeConfigureService runtimeConfigureService, SyncJobManager syncJobManager)
    {
        this.repository = repository;
        this.columnRepository = columnRepository;
        this.historyRepository = historyRepository;
        this.pluginManager = pluginManager;
        this.initializerConfigure = initializerConfigure;
        this.scheduler = scheduler;
        this.environment = environment;
        this.sourceRepository = sourceRepository;
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
        this.runtimeConfigureService = runtimeConfigureService;
        this.syncJobManager = syncJobManager;
    }

    @Override
    public CommonResponse<Long> count()
    {
        Long userId = UserDetailsService.getUser().getId();
        if (userId == null) {
            return CommonResponse.failure(ServiceState.USER_UNAUTHORIZED);
        }
        return CommonResponse.success(repository.countByUserId(userId));
    }

    @Transactional
    @Override
    public CommonResponse<DataSetEntity> saveOrUpdate(BaseRepository<DataSetEntity, Long> repository, DataSetEntity configure)
    {
        try {
            DatasetVirtualColumns.validate(configure.getColumns());
        }
        catch (IllegalArgumentException exception) {
            return CommonResponse.failure(exception.getMessage());
        }
        if (configure.getId() != null) {
            DataSetEntity original = repository.findById(configure.getId()).orElseThrow(() -> new IllegalArgumentException("Dataset not found"));
            isSelf(original);
            Set<Long> ownedColumns = columnRepository.findAllByDatasetId(original.getId()).stream().map(DataSetColumnEntity::getId).collect(Collectors.toSet());
            if (configure.getColumns().stream().anyMatch(column -> column.getId() != null && !ownedColumns.contains(column.getId()))) {
                return CommonResponse.failure("A column does not belong to this dataset");
            }
        }
        else if (configure.getColumns().stream().anyMatch(column -> column.getId() != null)) {
            return CommonResponse.failure("New datasets cannot reuse persisted column identities");
        }
        UserEntity user = UserDetailsService.getUser();
        // Decided before the build assigns an id, which made every new dataset read as an update.
        boolean created = configure.getId() == null;
        java.util.concurrent.ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            configure.setUser(user);
            completeState(configure, DataSetState.METADATA_START);
            startBuild(configure, true);
        });
        eventPublisher.publish(created ? NotificationScene.DATASET_CREATED : NotificationScene.DATASET_UPDATED, user,
                String.format(created ? "数据集「%s」已创建" : "数据集「%s」已更新", configure.getName()),
                String.format(created ? "数据集「%s」已创建，正在构建元数据。" : "数据集「%s」的配置已更新，正在重新构建。", configure.getName()),
                EntityType.DATASET, configure.getCode(), configure.getName());
        return CommonResponse.success(configure);
    }

    @Override
    public CommonResponse<Response> preview(String code)
    {
        return repository.findByCode(code).map(dataset -> {
            isSelf(dataset);
            if (dataset.getTableName() == null || dataset.getTableName().isBlank()) {
                return CommonResponse.<Response>failure("Dataset storage is not ready");
            }
            String database = initializerConfigure.getDataSetConfigure().getDatabase();
            String sql = DatasetVirtualColumns.previewSql(database, dataset.getTableName(), columnRepository.findAllByDatasetIdOrderByPositionAsc(dataset.getId()));
            String type = initializerConfigure.getDataSetConfigure().getType();
            return pluginManager.getPlugin(type).map(plugin -> {
                PluginService pluginService = plugin.getService(PluginService.class);
                Response response = pluginService.execute(getConfigure(database, plugin), sql);
                response.setContent(sql);
                return CommonResponse.success(response);
            }).orElseGet(() -> CommonResponse.failure("Dataset storage plugin not loaded: " + type));
        }).orElseGet(() -> CommonResponse.failure("Dataset not found: " + code));
    }

    @Override
    public CommonResponse<DataSetEntity> rebuild(String code)
    {
        return repository.findByCode(code)
                .map(entity -> {
                    java.util.concurrent.ExecutorService service = Executors.newSingleThreadExecutor();
                    service.submit(() -> startBuild(entity, false));
                    return CommonResponse.success(entity);
                })
                .orElse(CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<List<DataSetColumnEntity>> getColumnsByCode(String code)
    {
        return repository.findByCode(code)
                .map(item -> {
                    isSelf(item);
                    return CommonResponse.success(columnRepository.findAllByDatasetIdOrderByPositionAsc(item.getId()));
                })
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    /** 默认分片列：主键优先，其次排序键，取其源列名；都没有返回空串 / primary key first, then order-by key; empty when neither exists */
    private static String defaultSplitColumn(Iterable<DataSetColumnEntity> columns)
    {
        String orderBy = "";
        for (DataSetColumnEntity column : columns) {
            if (column.isVirtualColumn() || StringUtils.isBlank(column.getOriginal())) {
                continue;
            }
            if (column.isPrimaryKey()) {
                return column.getOriginal();
            }
            if (column.isOrderByKey() && orderBy.isEmpty()) {
                orderBy = column.getOriginal();
            }
        }
        return orderBy;
    }

    private static int parseIntOrDefault(String raw, int fallback)
    {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        }
        catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static void setAuthenticationContext(UserEntity user)
    {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode()))
                .collect(Collectors.toList());

        UserDetailsService userDetails = new UserDetailsService(
                user.getId(),
                user.getCode(),
                user.getUsername(),
                "", // 空密码，因为我们只需要身份信息
                authorities,
                null // 头像可能为空
        );

        // 创建认证对象
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, "", userDetails.getAuthorities());

        // 设置到 SecurityContext
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Override
    public CommonResponse<Boolean> clearData(String code)
    {
        return repository.findByCode(code)
                .map(item -> {
                    java.util.concurrent.ExecutorService service = Executors.newSingleThreadExecutor();
                    service.submit(() -> clearData(item, service));
                    return CommonResponse.success(true);
                })
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> adhoc(String code, Adhoc configure)
    {
        return repository.findByCode(code)
                .map(item -> {
                    String database = initializerConfigure.getDataSetConfigure().getDatabase();
                    List<SqlColumn> columns = Lists.newArrayList();
                    List<SqlColumn> groupBy = Lists.newArrayList();
                    List<SqlColumn> orderBy = Lists.newArrayList();
                    List<SqlColumn> wheres = Lists.newArrayList();
                    configure.getColumns()
                            .forEach(column -> columnRepository.findById(column.getId())
                                    .ifPresent(entity -> {
                                        AtomicReference<String> columnAlias = new AtomicReference<>(null);
                                        AtomicReference<String> expression = new AtomicReference<>(null);
                                        AtomicReference<String> columnName = new AtomicReference<>(entity.getName());
                                        if (StringUtils.isNotEmpty(column.getFunction())) {
                                            columnName.set(column.getFunction());
                                        }
                                        configure.getColumns().stream()
                                                .filter(it -> it.getId().equals(entity.getId()))
                                                .findFirst()
                                                .ifPresent(it -> {
                                                    expression.set(it.getExpression());
                                                    columnAlias.set(it.getAlias());
                                                });
                                        SqlColumn sqlColumn = SqlColumn.builder()
                                                .column(columnName.get())
                                                .expression(expression.get())
                                                .alias(columnAlias.get())
                                                .build();
                                        if (!column.getMode().equals(ColumnMode.FILTER)) {
                                            if (column.getMode().equals(ColumnMode.DIMENSION)) {
                                                sqlColumn.setExpression(null);
                                            }
                                            columns.add(sqlColumn);
                                        }
                                        // Only dimensions are added to GROUP BY
                                        if (entity.getMode().equals(ColumnMode.DIMENSION)) {
                                            if (!column.getMode().equals(ColumnMode.FILTER)) {
                                                groupBy.add(SqlColumn.builder()
                                                        .column(columnName.get())
                                                        .build());
                                            }
                                        }
                                        // Add to Sort Sequence
                                        if (StringUtils.isNotEmpty(column.getOrder())) {
                                            sqlColumn.setOrder(SqlOrder.valueOf(column.getOrder()));
                                            orderBy.add(sqlColumn);
                                        }
                                        // Add to where
                                        if (column.getMode().equals(ColumnMode.FILTER) && column.getExpression() != null) {
                                            sqlColumn.setOperator(SqlOperator.valueOf(column.getExpression()));
                                            sqlColumn.setValue(column.getValue());
                                            wheres.add(sqlColumn);
                                        }
                                    }));

                    String virtualTable = DatasetVirtualColumns.table(database, item.getTableName(),
                            columnRepository.findAllByDatasetIdOrderByPositionAsc(item.getId()),
                            configure.getColumns().stream().map(column -> column.getId()).collect(Collectors.toSet()));
                    SqlBody body = SqlBody.builder()
                            .type(SqlType.SELECT)
                            .database(virtualTable == null ? database : null)
                            .table(virtualTable == null ? item.getTableName() : virtualTable)
                            .columns(columns)
                            .groups(groupBy)
                            .orders(orderBy)
                            .where(wheres)
                            .condition(" AND ")
                            .limit(configure.getLimit())
                            .build();
                    String sql = new SqlBuilder(body).getSql();
                    log.info("Execute SQL: {} for DataSet [ {} ]", sql, code);

                    return pluginManager.getPlugin(initializerConfigure.getDataSetConfigure().getType())
                            .map(plugin -> {
                                PluginService pluginService = plugin.getService(PluginService.class);
                                SourceEntity entity = SourceEntity.builder()
                                        .host(initializerConfigure.getDataSetConfigure().getHost())
                                        .port(Integer.valueOf(initializerConfigure.getDataSetConfigure().getPort()))
                                        .username(initializerConfigure.getDataSetConfigure().getUsername())
                                        .password(initializerConfigure.getDataSetConfigure().getPassword())
                                        .database(database)
                                        .build();
                                Response response = pluginService.execute(entity.toConfigure(pluginManager, plugin), sql);
                                response.setContent(sql);
                                return CommonResponse.success(response);
                            })
                            .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", initializerConfigure.getDataSetConfigure().getType())));
                })
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Set<PluginMetadata>> getActuators()
    {
        Set<PluginMetadata> actuators = Sets.newHashSet();
        this.pluginManager.getPluginInfos()
                .stream()
                .filter(item -> item.getType().equals(org.devlive.metaforge.plugin.PluginType.EXECUTOR))
                .forEach(actuators::add);
        return CommonResponse.success(actuators);
    }

    @Override
    public CommonResponse<DataSetEntity> getInfo(String code)
    {
        return repository.findByCode(code)
                .map(Persistence.references()::load)
                .map(CommonResponse::success)
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<PageEntity<DatasetHistoryEntity>> getHistory(String code, FilterBody filter)
    {
        return repository.findByCode(code)
                .map(item -> {
                    Pageable pageable = PageRequestAdapter.of(filter);
                    return CommonResponse.success(PageEntity.build(Persistence.references().load(historyRepository.findAllByDatasetIdOrderByCreateTimeDesc(item.getId(), pageable))));
                })
                .orElse(CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<List<String>> getHistoryLog(Long id)
    {
        return historyRepository.findById(id)
                .map(history -> {
                    String workHome = history.getWorkHome();
                    String taskName = history.getTaskName();
                    if (StringUtils.isBlank(workHome) || StringUtils.isBlank(taskName)) {
                        return CommonResponse.<List<String>>failure("Sync history has no log location (older record or non-local executor)");
                    }
                    // 防穿越：拼好后规整为绝对路径，并校验仍在 dataHome 下
                    java.io.File logFile = new java.io.File(workHome, taskName + ".log");
                    String dataHome = initializerConfigure.getDataHome();
                    try {
                        String canonicalLog = logFile.getCanonicalPath();
                        String canonicalHome = new java.io.File(dataHome).getCanonicalPath();
                        if (!canonicalLog.startsWith(canonicalHome)) {
                            return CommonResponse.<List<String>>failure("Illegal log path");
                        }
                    }
                    catch (java.io.IOException ex) {
                        return CommonResponse.<List<String>>failure("Resolve log path failed: " + ex.getMessage());
                    }
                    if (!logFile.exists()) {
                        return CommonResponse.<List<String>>success(Lists.<String>newArrayList());
                    }
                    try (java.io.FileInputStream stream = new java.io.FileInputStream(logFile)) {
                        List<String> lines = org.apache.commons.io.IOUtils.readLines(stream, java.nio.charset.StandardCharsets.UTF_8);
                        return CommonResponse.<List<String>>success(lines);
                    }
                    catch (java.io.IOException ex) {
                        log.error("Read sync log [ {} ] failed", logFile.getAbsolutePath(), ex);
                        return CommonResponse.<List<String>>failure("Read log file failed: " + ex.getMessage());
                    }
                })
                .orElseGet(() -> CommonResponse.<List<String>>failure(String.format("Sync history [ %d ] not found", id)));
    }

    @Override
    public CommonResponse<Boolean> stopHistory(Long id)
    {
        log.info("Stop history [ {} ] requested", id);
        Optional<SyncJob> active = syncJobManager.find(id);
        if (active.isEmpty()) {
            return historyRepository.findById(id)
                    .map(history -> CommonResponse.<Boolean>failure(RunStateMachine.isTerminal(history.getState())
                            ? String.format("Sync history [ %d ] is not running (current state: %s)", id, history.getState())
                            : String.format("Sync history [ %d ] is not running on this node", id)))
                    .orElseGet(() -> CommonResponse.<Boolean>failure(String.format("Sync history [ %d ] not found", id)));
        }
        SyncJob job = active.get();
        if (syncJobManager.cancelQueued(job)) {
            return CommonResponse.success(Boolean.TRUE);
        }
        // 正在等待重试：工作线程醒来发现已是终态就结束 / waiting to retry: the worker sees the final state and ends
        if (job.getState() == RunState.RETRYING && job.transition(RunState.STOPPED, "Cancelled while waiting to retry")) {
            return CommonResponse.success(Boolean.TRUE);
        }
        if (job.getState() != RunState.RUNNING && job.getState() != RunState.PAUSING) {
            return CommonResponse.failure(String.format("Sync history [ %d ] cannot be cancelled (current state: %s)", id, job.getState()));
        }
        return historyRepository.findById(id)
                .map(history -> {
                    log.info("Stop history [ {} ] state={} taskName={}", id, job.getState(), history.getTaskName());
                    String taskName = history.getTaskName();
                    if (StringUtils.isBlank(taskName)) {
                        return CommonResponse.<Boolean>failure("Sync history has no taskName (older record), cannot stop");
                    }
                    DataSetEntity dataset = history.getDatasetId() == null ? null : repository.findById(history.getDatasetId()).orElse(null);
                    if (dataset == null || StringUtils.isBlank(dataset.getExecutor())) {
                        return CommonResponse.<Boolean>failure("Sync history has no associated dataset executor");
                    }
                    log.info("Stop history [ {} ] resolving executor plugin [ {} ]", id, dataset.getExecutor());
                    Optional<org.devlive.metaforge.plugin.Plugin> executorPlugin = pluginManager.getPlugin(dataset.getExecutor());
                    if (executorPlugin.isEmpty()) {
                        return CommonResponse.<Boolean>failure(String.format("Executor [ %s ] not found", dataset.getExecutor()));
                    }
                    ExecutorService executorService = executorPlugin.get().getService(ExecutorService.class);
                    // stop 只需要 taskName 定位活跃任务，input/output 用空 configure 占位
                    ExecutorConfigure stopPlaceholder = new ExecutorConfigure(null);
                    ExecutorRequest stopRequest = new ExecutorRequest(taskName, "", stopPlaceholder, stopPlaceholder);
                    log.info("Stop history [ {} ] calling executor.stop(taskName={})", id, taskName);
                    ExecutorResponse stopResponse = executorService.stop(stopRequest);
                    log.info("Stop history [ {} ] executor returned successful={} message={}", id, stopResponse.getSuccessful(), stopResponse.getMessage());
                    if (Boolean.TRUE.equals(stopResponse.getSuccessful())) {
                        // 让 UI 立刻看到 STOPPING 中间态，直到执行器真正停下
                        job.transition(RunState.STOPPING, null);
                        return CommonResponse.success(Boolean.TRUE);
                    }
                    return CommonResponse.<Boolean>failure(stopResponse.getMessage() == null ? "Stop failed" : stopResponse.getMessage());
                })
                .orElseGet(() -> CommonResponse.<Boolean>failure(String.format("Sync history [ %d ] not found", id)));
    }

    @Override
    public CommonResponse<Boolean> pauseHistory(Long id)
    {
        Optional<SyncJob> active = syncJobManager.find(id);
        if (active.isEmpty() || active.get().getState() != RunState.RUNNING) {
            return CommonResponse.failure(String.format("Sync history [ %d ] is not running", id));
        }
        SyncJob job = active.get();
        Optional<DataSetEntity> dataset = repository.findById(job.getDatasetId());
        if (dataset.isEmpty()) {
            return CommonResponse.failure(String.format("Sync history [ %d ] has no dataset", id));
        }
        isSelf(dataset.get());
        String taskName = job.getTaskName();
        if (StringUtils.isBlank(taskName)) {
            return CommonResponse.failure("The sync is still starting, try again in a moment");
        }
        return pluginManager.getPlugin(dataset.get().getExecutor())
                .map(plugin -> {
                    ExecutorConfigure placeholder = new ExecutorConfigure(null);
                    ExecutorResponse response = plugin.getService(ExecutorService.class).pause(new ExecutorRequest(taskName, "", placeholder, placeholder));
                    if (!Boolean.TRUE.equals(response.getSuccessful())) {
                        return CommonResponse.<Boolean>failure(response.getMessage() == null ? "Pause failed" : response.getMessage());
                    }
                    // 暂停可能已经完成，此时这次转换被拒绝，不影响结果 / the pause may already be done; the rejected move changes nothing
                    job.transition(RunState.PAUSING, null);
                    return CommonResponse.success(Boolean.TRUE);
                })
                .orElseGet(() -> CommonResponse.<Boolean>failure(String.format("Executor [ %s ] not found", dataset.get().getExecutor())));
    }

    @Override
    public CommonResponse<Boolean> resumeHistory(Long id)
    {
        Optional<SyncJob> active = syncJobManager.find(id);
        if (active.isEmpty() || active.get().getState() != RunState.PAUSED) {
            return CommonResponse.failure(String.format("Sync history [ %d ] is not paused", id));
        }
        repository.findById(active.get().getDatasetId()).ifPresent(this::isSelf);
        return syncJobManager.resume(active.get())
                ? CommonResponse.success(Boolean.TRUE)
                : CommonResponse.failure(String.format("Sync history [ %d ] is not paused", id));
    }

    @Override
    public CommonResponse<Boolean> retryHistory(Long id)
    {
        Optional<DatasetHistoryEntity> found = historyRepository.findById(id);
        if (found.isEmpty()) {
            return CommonResponse.failure(String.format("Sync history [ %d ] not found", id));
        }
        DatasetHistoryEntity history = found.get();
        if (!SyncJobManager.canRetry(history)) {
            return CommonResponse.failure(String.format(
                    "Sync history [ %d ] cannot run again: it has no checkpoint and running it again could write rows twice. Clear the data and sync again.", id));
        }
        Optional<DataSetEntity> dataset = repository.findById(history.getDatasetId());
        if (dataset.isEmpty()) {
            return CommonResponse.failure(String.format("Sync history [ %d ] has no dataset", id));
        }
        isSelf(dataset.get());
        return syncJobManager.retry(history, syncBody(dataset.get(), UserDetailsService.getUser(), java.util.Collections.emptyMap()))
                .map(job -> CommonResponse.success(Boolean.TRUE))
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] already has an unfinished sync", dataset.get().getName())));
    }

    @Override
    public CommonResponse<SyncMetricsView> getHistoryMetrics(Long id)
    {
        return syncJobManager.view(id)
                .map(CommonResponse::success)
                .orElseGet(() -> CommonResponse.failure(String.format("Sync history [ %d ] not found", id)));
    }

    @Override
    @Transactional
    public CommonResponse<String> deleteByCode(BaseRepository<DataSetEntity, Long> repository, String code)
    {
        return repository.findByCode(code)
                .map(value -> {
                    java.util.concurrent.ExecutorService service = Executors.newSingleThreadExecutor();
                    service.submit(() -> {
                        // 更新通知状态，标记为无法点击链接
                        notificationRepository.updateEntityExistsByEntityCodeAndType(
                                value.getCode(),
                                EntityType.DATASET,
                                false
                        );

                        pluginManager.getPlugin(initializerConfigure.getDataSetConfigure().getType())
                                .ifPresent(plugin -> {
                                    PluginService pluginService = plugin.getService(PluginService.class);
                                    TableDefinition definition = TableDefinition.create(initializerConfigure.getDataSetConfigure().getDatabase(), value.getTableName());
                                    pluginService.dropTable(getConfigure(initializerConfigure.getDataSetConfigure().getDatabase(), plugin), definition);
                                });
                        columnRepository.deleteAllByDatasetId(value.getId());
                        historyRepository.deleteAllByDatasetId(value.getId());
                        repository.delete(value);
                    });
                    eventPublisher.publish(NotificationScene.DATASET_DELETED, String.format("数据集「%s」已删除", value.getName()),
                            String.format("数据集「%s」及其同步数据已删除。", value.getName()), EntityType.DATASET, value.getCode(), value.getName());
                    return CommonResponse.success(code);
                })
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    private void completeState(DataSetEntity entity, DataSetState state)
    {
        List<DataSetState> sourceStates = entity.getState();
        List<DataSetState> states = Lists.newArrayList();
        if (sourceStates != null) {
            states = sourceStates;
            states = states.stream().filter(item -> !item.name().startsWith(state.name().split("_")[0])).collect(Collectors.toList());
        }
        states.add(state);
        entity.setState(states);
    }

    private String getColumnType(ColumnType type)
    {
        switch (type) {
            case NUMBER:
                return "Bigint";
            case NUMBER_SIGNED:
                return "UInt64";
            case BOOLEAN:
                return "Boolean";
            case DATETIME:
                return "DateTime";
            case STRING:
            default:
                return "String";
        }
    }

    private RuntimeConfigure getConfigure(String database, Plugin plugin)
    {
        return RuntimeConfigure.builder()
                .host(initializerConfigure.getDataSetConfigure().getHost())
                .port(Integer.valueOf(initializerConfigure.getDataSetConfigure().getPort()))
                .username(Optional.ofNullable(initializerConfigure.getDataSetConfigure().getUsername()))
                .password(Optional.ofNullable(initializerConfigure.getDataSetConfigure().getPassword()))
                .database(Optional.ofNullable(database))
                .pluginManager(pluginManager)
                .format("JsonConvert")
                .plugin(plugin)
                .ssl(Optional.empty())
                .env(Optional.empty())
                .build();
    }

    private void startBuild(DataSetEntity entity, boolean rebuildColumn)
    {
        Optional<SourceEntity> requested = entity.getSource() != null && entity.getSource().getCode() != null
                ? sourceRepository.findByCode(entity.getSource().getCode())
                : Optional.ofNullable(entity.getSourceId()).flatMap(sourceRepository::findById);
        requested
                .ifPresentOrElse(
                        source -> {
                            if (entity.getId() == null) {
                                entity.setCode(UUID.randomUUID().toString());
                                String tablePrefix = initializerConfigure.getDataSetConfigure().getTablePrefix();
                                String tableName = String.format("%s%s", tablePrefix, UUID.randomUUID().toString().replace("-", ""));
                                entity.setTableName(tableName);
                            }
                            log.info("Start build metadata for dataset [ {} ] id [ {} ]", entity.getName(), entity.getId());
                            entity.setSource(source);
                            createMetadata(entity, rebuildColumn);
                        },
                        () -> {
                            entity.setMessage("Source [ %s ] not found");
                            entity.setState(Lists.newArrayList(DataSetState.METADATA_FAILED));
                            repository.save(entity);
                        }
                );
    }

    private void createMetadata(DataSetEntity entity, boolean rebuildColumn)
    {
        Set<DataSetColumnEntity> targetColumns = entity.getColumns();
        Set<CreatedModel> createdModels = this.createdModeProcess(targetColumns, entity);
        try {
            repository.save(entity);
            if (rebuildColumn) {
                entity.getColumns()
                        .forEach(item -> item.setDataset(DataSetEntity.builder().id(entity.getId()).build()));
                columnRepository.saveAll(entity.getColumns());

                List<DataSetColumnEntity> originalColumns = columnRepository.findAllByDatasetId(entity.getId());
                List<DataSetColumnEntity> columnsNotInEntity = originalColumns.stream()
                        .filter(originalColumn -> entity.getColumns().stream()
                                .noneMatch(addedColumn -> addedColumn.getId().equals(originalColumn.getId())))
                        .collect(Collectors.toList());
                columnRepository.deleteAll(columnsNotInEntity);
            }
            completeState(entity, DataSetState.METADATA_SUCCESS);
        }
        catch (Exception e) {
            log.warn("Create dataset [ {} ] ", entity.getName(), e);
            completeState(entity, DataSetState.METADATA_FAILED);
            entity.setMessage(e.getMessage());
        }
        finally {
            repository.save(entity);
            DataSetState state = entity.getState().get(entity.getState().size() - 1);
            if (state.equals(DataSetState.METADATA_SUCCESS)
                    || state.equals(DataSetState.TABLE_START)
                    || state.equals(DataSetState.TABLE_FAILED)) {
                log.info("Start build table for dataset [ {} ]", entity.getName());
                Optional<CreatedModel> createdModel = createdModels.stream()
                        .filter(item -> item.getMode().equals(CreatedMode.CREATE_TABLE))
                        .findFirst();
                if (createdModel.isPresent()) {
                    createTable(entity);
                }
                else {
                    createAndModifyColumn(entity, createdModels);
                }
            }
        }
    }

    private void createTable(DataSetEntity entity)
    {
        try {
            pluginManager.getPlugin(initializerConfigure.getDataSetConfigure().getType())
                    .ifPresentOrElse(plugin -> {
                                PluginService pluginService = plugin.getService(PluginService.class);
                                String database = initializerConfigure.getDataSetConfigure().getDatabase();
                                String originTableName = entity.getTableName();
                                String tableDefaultEngine = initializerConfigure.getDataSetConfigure().getTableDefaultEngine();

                                List<Column> columns = Lists.newArrayList();
                                List<DataSetColumnEntity> columnEntities = columnRepository.findAllByDatasetId(entity.getId());
                                columnEntities.stream()
                                        .filter(item -> !item.isVirtualColumn())
                                        .forEach(item -> {
                                            Column column = new Column();
                                            column.setName(item.getName());
                                            column.setType(getColumnType(item.getType()));
                                            column.setComment(item.getComment());
                                            column.setLength(item.getLength());
                                            column.setNullable(item.isNullable());
                                            column.setDefaultValue(item.getDefaultValue());
                                            columns.add(column);
                                        });

                                TableBuilder.Companion.BEGIN();
                                TableBuilder.Companion.CREATE_TABLE(String.format("`%s`.`%s`", database, originTableName));
                                TableBuilder.Companion.COLUMNS(columns.stream().map(Column::toColumnVar).collect(Collectors.toList()));
                                TableBuilder.Companion.ENGINE(tableDefaultEngine);
                                TableBuilder.Companion.ORDER_BY(columnEntities.stream().filter(DataSetColumnEntity::isOrderByKey).map(DataSetColumnEntity::getName).collect(Collectors.toList()));
                                TableBuilder.Companion.PARTITION_BY(columnEntities.stream().filter(DataSetColumnEntity::isPartitionKey).map(DataSetColumnEntity::getName).collect(Collectors.toList()));
                                TableBuilder.Companion.PRIMARY_KEY(columnEntities.stream().filter(DataSetColumnEntity::isPrimaryKey).map(DataSetColumnEntity::getName).collect(Collectors.toList()));
                                TableBuilder.Companion.SAMPLING_KEY(columnEntities.stream().filter(DataSetColumnEntity::isSamplingKey).map(DataSetColumnEntity::getName).collect(Collectors.toList()));
                                if (entity.getLifeCycleColumn() != null
                                        && entity.getLifeCycle() != null
                                        && entity.getLifeCycleType() != null) {
                                    TableBuilder.Companion.ADD_LIFECYCLE(String.format("`%s` + INTERVAL %s %s", entity.getLifeCycleColumn(), entity.getLifeCycle(), entity.getLifeCycleType()));
                                }
                                String sql = TableBuilder.Companion.SQL();
                                log.info("Create table sql \n {} \n on dataset [ {} ]", sql, entity.getName());

                                RuntimeConfigure targetConfigure = new RuntimeConfigure();
                                targetConfigure.setHost(initializerConfigure.getDataSetConfigure().getHost());
                                targetConfigure.setPort(Integer.valueOf(initializerConfigure.getDataSetConfigure().getPort()));
                                targetConfigure.setUsername(Optional.ofNullable(initializerConfigure.getDataSetConfigure().getUsername()));
                                targetConfigure.setPassword(Optional.ofNullable(initializerConfigure.getDataSetConfigure().getPassword()));
                                targetConfigure.setDatabase(Optional.ofNullable(database));
                                targetConfigure.setPlugin(plugin);
                                targetConfigure.setPluginManager(pluginManager);
                                Response response = pluginService.execute(targetConfigure, sql);
                                if (response.getIsSuccessful()) {
                                    entity.setTableName(originTableName);
                                    entity.setMessage(null);
                                    completeState(entity, DataSetState.TABLE_SUCCESS);
                                }
                                else {
                                    throw new RuntimeException(response.getMessage());
                                }
                            },
                            () -> {
                                String type = initializerConfigure.getDataSetConfigure().getType();
                                String errorMessage = String.format("Plugin [ %s ] not found", type);
                                log.error(errorMessage);
                                throw new IllegalArgumentException(errorMessage);
                            });
        }
        catch (Exception e) {
            log.warn("Create dataset [ {} ] ", entity.getName(), e);
            completeState(entity, DataSetState.TABLE_FAILED);
            entity.setMessage(e.getMessage());
        }
        finally {
            repository.save(entity);
            DataSetState state = entity.getState().get(entity.getState().size() - 1);
            if (state.equals(DataSetState.TABLE_SUCCESS)) {
                flushSyncData(entity);
            }
        }
    }

    private void createAndModifyColumn(DataSetEntity entity, Set<CreatedModel> createdModels)
    {
        try {
            pluginManager.getPlugin(initializerConfigure.getDataSetConfigure().getType())
                    .ifPresentOrElse(
                            plugin -> {
                                String tableName = String.format("`%s`.`%s`", initializerConfigure.getDataSetConfigure().getDatabase(), entity.getTableName());

                                List<Column> createColumns = createdModels.stream()
                                        .filter(item -> item.getMode().equals(CreatedMode.CREATE_COLUMN))
                                        .map(this::formatColumn)
                                        .collect(Collectors.toList());
                                if (!createColumns.isEmpty()) {
                                    ColumnBuilder.Companion.BEGIN();
                                    ColumnBuilder.Companion.CREATE_COLUMN(tableName);
                                    ColumnBuilder.Companion.COLUMNS(createColumns.stream().map(Column::toColumnVar).collect(Collectors.toList()));
                                    ColumnBuilder.Companion.FORMAT_ENGINE(EngineType.CLICKHOUSE);
                                    String sql = ColumnBuilder.Companion.SQL();
                                    PluginService pluginService = getOutputPlugin();
                                    SourceEntity source = getOutputSource();
                                    Response response = pluginService.execute(source.toConfigure(pluginManager, plugin), sql);
                                    Preconditions.checkArgument(response.getIsSuccessful(), response.getMessage());
                                    log.info("Create column sql \n {} \n on dataset [ {} ] id [ {} ]", sql, entity.getName(), entity.getId());
                                }

                                List<Column> modifyColumns = createdModels.stream()
                                        .filter(item -> item.getMode().equals(CreatedMode.MODIFY_COLUMN))
                                        .map(this::formatColumn)
                                        .collect(Collectors.toList());
                                if (!modifyColumns.isEmpty()) {
                                    ColumnBuilder.Companion.BEGIN();
                                    ColumnBuilder.Companion.MODIFY_COLUMN(tableName);
                                    ColumnBuilder.Companion.COLUMNS(modifyColumns.stream().map(Column::toColumnVar).collect(Collectors.toList()));
                                    ColumnBuilder.Companion.FORMAT_ENGINE(EngineType.CLICKHOUSE);
                                    String sql = ColumnBuilder.Companion.SQL();
                                    log.info("Modify column sql \n {} \n on dataset [ {} ] id [ {} ]", sql, entity.getName(), entity.getId());
                                    PluginService pluginService = getOutputPlugin();
                                    SourceEntity source = getOutputSource();
                                    Response response = pluginService.execute(source.toConfigure(pluginManager, plugin), sql);
                                    Preconditions.checkArgument(response.getIsSuccessful(), response.getMessage());
                                }

                                createdModels.stream()
                                        .filter(item -> item.getMode().equals(CreatedMode.MODIFY_LIFECYCLE))
                                        .findFirst()
                                        .ifPresent(item -> {
                                            TableBuilder.Companion.BEGIN();
                                            TableBuilder.Companion.MODIFY_LIFECYCLE(tableName);
                                            TableBuilder.Companion.LIFECYCLE(String.format("`%s` + INTERVAL %s %s", item.getColumn().getName(), item.getColumn().getLength(), item.getColumn().getDefaultValue()));
                                            String sql = TableBuilder.Companion.SQL();
                                            log.info("Modify lifecycle sql \n {} \n on dataset [ {} ] id [ {} ]", sql, entity.getName(), entity.getId());
                                            PluginService pluginService = getOutputPlugin();
                                            SourceEntity source = getOutputSource();
                                            Response response = pluginService.execute(source.toConfigure(pluginManager, plugin), sql);
                                            Preconditions.checkArgument(response.getIsSuccessful(), response.getMessage());
                                        });

                                completeState(entity, DataSetState.TABLE_SUCCESS);
                            },
                            () -> {
                                entity.setMessage(String.format("Plugin [ %s ] not found", initializerConfigure.getDataSetConfigure().getType()));
                                completeState(entity, DataSetState.COMPLETE_FAILED);
                            }
                    );
        }
        catch (Exception e) {
            log.warn("Modify dataset [ {} ] id [ {} ] add column failed", entity.getName(), entity.getId(), e);
            completeState(entity, DataSetState.TABLE_FAILED);
            entity.setMessage(e.getMessage());
        }
        finally {
            repository.save(entity);
            DataSetState state = entity.getState().get(entity.getState().size() - 1);
            if (state.equals(DataSetState.TABLE_SUCCESS)) {
                flushSyncData(entity);
            }
        }
    }

    /**
     * Records how the latest sync left the dataset's data: a successful sync completes the dataset,
     * a failed one marks its data failed and withdraws the completion. A stop the user asked for
     * leaves the state as it was. The dataset is re-read so fields saved during the sync survive.
     */
    private void recordDataState(Long datasetId, SyncJob job)
    {
        RunState state = job.getState();
        if (datasetId == null || state != RunState.SUCCESS && state != RunState.FAILURE && state != RunState.TIMEOUT) {
            return;
        }
        List<org.devlive.metaforge.service.dataset.sync.SyncTransition> transitions = job.getTransitions();
        String reason = transitions.isEmpty() ? null : transitions.get(transitions.size() - 1).getMessage();
        repository.findById(datasetId).ifPresent(dataset -> {
            if (state == RunState.SUCCESS) {
                completeState(dataset, DataSetState.DATA_SUCCESS);
                completeState(dataset, DataSetState.COMPLETE_SUCCESS);
                dataset.setMessage(null);
            }
            else {
                dataset.setState(dataset.getState().stream().filter(item -> !item.name().startsWith("COMPLETE")).collect(Collectors.toList()));
                completeState(dataset, DataSetState.DATA_FAILED);
                // The dataset's error view shows why the latest sync failed.
                dataset.setMessage(reason == null || reason.isBlank() ? state.name() : reason);
            }
            repository.save(dataset);
        });
    }

    /** Tells the user how a sync ended; one they stopped themselves needs no notice. */
    private void notifySynced(DataSetEntity entity, SyncJob job, UserEntity user)
    {
        RunState state = job.getState();
        if (state != RunState.SUCCESS && state != RunState.FAILURE && state != RunState.TIMEOUT) {
            return;
        }
        boolean succeeded = state == RunState.SUCCESS;
        List<org.devlive.metaforge.service.dataset.sync.SyncTransition> transitions = job.getTransitions();
        String reason = transitions.isEmpty() ? null : transitions.get(transitions.size() - 1).getMessage();
        String content = succeeded
                ? String.format("数据集「%s」同步完成。", entity.getName())
                : String.format("数据集「%s」同步失败：%s", entity.getName(), reason == null || reason.isBlank() ? state.name() : reason);
        eventPublisher.publish(succeeded ? NotificationScene.DATASET_SYNC_SUCCEEDED : NotificationScene.DATASET_SYNC_FAILED, user,
                String.format(succeeded ? "数据集「%s」同步成功" : "数据集「%s」同步失败", entity.getName()),
                content, EntityType.DATASET, entity.getCode(), entity.getName());
    }

    private PluginService getOutputPlugin()
    {
        return pluginManager.getPlugin(initializerConfigure.getDataSetConfigure().getType())
                .map(plugin -> plugin.getService(PluginService.class))
                .orElseThrow(() -> new RuntimeException("Not found service from %s" + initializerConfigure.getDataSetConfigure().getType()));
    }

    @Override
    public CommonResponse<DatasetHistoryEntity> syncData(String code)
    {
        return syncData(code, null);
    }

    @Override
    public CommonResponse<DatasetHistoryEntity> syncData(String code, java.util.Map<String, String> overrides)
    {
        return repository.findByCode(code)
                .map(entity -> {
                    UserEntity currentUser = UserDetailsService.getUser();
                    java.util.Map<String, String> safeOverrides = overrides == null
                            ? java.util.Collections.emptyMap()
                            : new java.util.HashMap<>(overrides);
                    AtomicReference<DatasetHistoryEntity> created = new AtomicReference<>();
                    Optional<SyncJob> job = syncJobManager.submit(entity.getId(), () -> {
                        DatasetHistoryEntity history = new DatasetHistoryEntity();
                        history.setState(RunState.CREATED);
                        history.setCreateTime(new Date());
                        history.setQuery(entity.getQuery());
                        history.setMode(QueryMode.SYNC);
                        history.setAttempt(1);
                        history.setDataset(entity);
                        history.setDatasetId(entity.getId());
                        created.set(historyRepository.save(history));
                        return created.get();
                    }, syncBody(entity, currentUser, safeOverrides));
                    return job.map(value -> CommonResponse.success(created.get()))
                            .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] already has an unfinished sync", entity.getName())));
                })
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<java.util.List<org.devlive.metaforge.plugin.configure.RuntimeConfigureField>> getSyncFields(String code)
    {
        return repository.findByCode(code)
                .<CommonResponse<java.util.List<org.devlive.metaforge.plugin.configure.RuntimeConfigureField>>>map(entity -> {
                    return pluginManager.getPlugin(entity.getExecutor())
                            .map(plugin -> {
                                java.util.List<org.devlive.metaforge.plugin.configure.RuntimeConfigureField> schema = plugin.runtimeConfigures();
                                java.util.Map<String, String> effective = runtimeConfigureService.getEffective(
                                        RuntimeConfigureService.CATEGORY_EXECUTOR,
                                        plugin.getName(),
                                        schema
                                );
                                java.util.List<org.devlive.metaforge.plugin.configure.RuntimeConfigureField> tunable = schema.stream()
                                        .filter(org.devlive.metaforge.plugin.configure.RuntimeConfigureField::isTunable)
                                        .map(f -> new org.devlive.metaforge.plugin.configure.RuntimeConfigureField(
                                                f.getName(),
                                                f.getType(),
                                                effective.getOrDefault(f.getName(), f.getDefaultValue()),
                                                f.getDescription(),
                                                true,
                                                f.getOptions(),
                                                f.getGroup()
                                        ))
                                        .collect(java.util.stream.Collectors.toList());
                                return CommonResponse.success(tunable);
                            })
                            .orElseGet(() -> CommonResponse.<java.util.List<org.devlive.metaforge.plugin.configure.RuntimeConfigureField>>failure(
                                    String.format("Executor [ %s ] not found", entity.getExecutor())));
                })
                .orElseGet(() -> CommonResponse.<java.util.List<org.devlive.metaforge.plugin.configure.RuntimeConfigureField>>failure(
                        String.format("DataSet [ %s ] not found", code)));
    }

    /** 同步主体：在工作线程运行；继续或重试时也用它 / the sync body, run on a worker thread; resuming and retrying reuse it */
    private java.util.function.Consumer<SyncJob> syncBody(DataSetEntity entity, UserEntity user, java.util.Map<String, String> overrides)
    {
        return running -> {
            // 在工作线程中设置用户上下文，结束后清理，线程会被复用
            setAuthenticationContext(user);
            try {
                DataSetEntity result = runSync(entity, running, overrides);
                // 暂停还没结束，继续后才记录和通知 / a paused sync has not finished, so this waits until it does
                if (RunStateMachine.isTerminal(running.getState())) {
                    recordDataState(result.getId(), running);
                    notifySynced(result, running, user);
                }
            }
            finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    /**
     * Flushes the synchronous data for the given DataSetEntity.
     *
     * @param entity the DataSetEntity to flush
     */
    private void flushSyncData(DataSetEntity entity)
    {
        log.info("Start schedule for dataset [ {} ] id [ {} ]", entity.getName(), entity.getId());
        if (entity.getSyncMode().equals(SyncMode.TIMING)) {
            pluginManager.getPlugin(entity.getScheduler())
                    .ifPresent(plugin -> {
                        SchedulerRequest request = SchedulerRequest.builder()
                                .name(entity.getCode())
                                .group("metaforge")
                                .expression(entity.getExpression())
                                .jobId(entity.getCode())
                                .createBeforeDelete(true)
                                .build();

                        SchedulerService schedulerService = plugin.getService(SchedulerService.class);
                        if (entity.getScheduler().equalsIgnoreCase("LocalScheduler")) {
                            request.setJob(new DatasetJob());
                            request.setScheduler(this.scheduler);
                        }
                        schedulerService.initialize(request);
                    });
        }
        else {
            pluginManager.getPlugin(entity.getScheduler())
                    .ifPresent(plugin -> {
                        SchedulerRequest request = SchedulerRequest.builder()
                                .name(entity.getCode())
                                .group("metaforge")
                                .build();

                        SchedulerService schedulerService = plugin.getService(SchedulerService.class);
                        if (entity.getScheduler().equalsIgnoreCase("LocalScheduler")) {
                            request.setScheduler(this.scheduler);
                        }
                        schedulerService.stop(request);
                    });
        }
    }

    private void clearData(DataSetEntity entity, java.util.concurrent.ExecutorService service)
    {
        try {
            pluginManager.getPlugin(initializerConfigure.getDataSetConfigure().getType())
                    .ifPresent(plugin -> {
                        SourceEntity source = SourceEntity.builder()
                                .type(initializerConfigure.getDataSetConfigure().getType())
                                .host(initializerConfigure.getDataSetConfigure().getHost())
                                .port(Integer.valueOf(initializerConfigure.getDataSetConfigure().getPort()))
                                .database(initializerConfigure.getDataSetConfigure().getDatabase())
                                .username(initializerConfigure.getDataSetConfigure().getUsername())
                                .password(initializerConfigure.getDataSetConfigure().getPassword())
                                .protocol(PluginType.JDBC.name())
                                .build();

                        PluginService pluginService = plugin.getService(PluginService.class);
                        SqlBody body = SqlBody.builder()
                                .type(SqlType.TRUNCATE)
                                .database(initializerConfigure.getDataSetConfigure().getDatabase())
                                .table(entity.getTableName())
                                .build();
                        String sql = new SqlBuilder(body).getSql();
                        log.info("Clear data for dataset [ {} ] id [ {} ] sql \n {}", entity.getName(), entity.getId(), sql);
                        Response response = pluginService.execute(source.toConfigure(pluginManager, plugin), sql);
                        Preconditions.checkArgument(response.getIsSuccessful(), response.getMessage());
                        this.flushTableMetadata(
                                entity,
                                pluginService,
                                initializerConfigure.getDataSetConfigure().getDatabase(),
                                source.toConfigure(pluginManager, plugin),
                                plugin
                        );
                    });
        }
        catch (Exception e) {
            log.warn("Clear data for dataset [ {} ] failed", entity.getName(), e);
        }
        finally {
            service.shutdownNow();
        }
    }

    /**
     * Flushes the table metadata for a given dataset entity using the specified plugin and database configuration.
     *
     * @param entity the dataset entity to flush
     * @param pluginService the plugin used to connect
     * @param database the database name
     * @param configure the configuration settings
     */
    private void flushTableMetadata(DataSetEntity entity, PluginService pluginService, String database, RuntimeConfigure configure, Plugin plugin)
    {
        // Get the total number of rows and the total size of the dataset
        log.info("Get the total number of rows and the total size of the dataset [ {} ]", entity.getName());
        SqlBuilder builder = new SqlBuilder(SqlBody.builder()
                .type(SqlType.SELECT)
                .database("system")
                .table("parts")
                .columns(
                        Lists.newArrayList(
                                SqlColumn.builder().column("SUM(rows) AS totalCount").build(),
                                SqlColumn.builder().column("formatReadableSize(SUM(data_compressed_bytes)) AS totalSize").build()
                        )
                )
                .where(
                        Lists.newArrayList(
                                SqlColumn.builder().column("database").operator(SqlOperator.EQ).value(database).build(),
                                SqlColumn.builder().column("table").operator(SqlOperator.EQ).value(entity.getTableName()).build(),
                                SqlColumn.builder().column("active").operator(SqlOperator.EQ).value("1").build()
                        )
                )
                .condition(" AND ")
                .build());
        configure.setFormat("NoneConvert");
        configure.setPluginManager(pluginManager);
        configure.setPlugin(plugin);
        Response outputResponse = pluginService.execute(configure, builder.getSql());
        if (outputResponse.getIsSuccessful()) {
            Object columnData = outputResponse.getColumns().get(0);
            if (columnData instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> data = (List<Object>) columnData;
                if (!data.isEmpty()) {
                    entity.setTotalRows(Long.parseLong(String.valueOf(data.get(0)).trim()));
                    entity.setTotalSize(data.get(1).toString());
                    repository.save(entity);
                }
            }
            else {
                log.warn("The response data is not a list type");
            }
        }
    }

    private Set<CreatedModel> createdModeProcess(Set<DataSetColumnEntity> targetColumns, DataSetEntity entity)
    {
        Set<CreatedModel> models = Sets.newHashSet();
        try {
            // If the id tag data is not set to new
            if (entity.getId() == null) {
                models.add(new CreatedModel(null, CreatedMode.CREATE_TABLE));
                return models;
            }

            // Check whether there is a data table bound to the current dataset
            if (!checkTableExists(entity)) {
                models.add(new CreatedModel(null, CreatedMode.CREATE_TABLE));
                return models;
            }

            List<DataSetColumnEntity> sourceColumns = columnRepository.findAllByDatasetId(entity.getId())
                    .stream()
                    .filter(item -> !item.isVirtualColumn())
                    .collect(Collectors.toList());
            targetColumns = targetColumns.stream()
                    .filter(item -> !item.isVirtualColumn())
                    .collect(Collectors.toSet());
            for (DataSetColumnEntity sourceColumn : sourceColumns) {
                Optional<DataSetColumnEntity> filterColumn = targetColumns.stream()
                        .filter(item -> item.getId().equals(sourceColumn.getId()))
                        .findFirst();
                if (filterColumn.isEmpty()) {
                    models.add(new CreatedModel(sourceColumn, CreatedMode.CREATE_COLUMN));
                }
                else {
                    boolean matchFound = targetColumns.stream()
                            .filter(item -> item.getId().equals(sourceColumn.getId()))
                            .anyMatch(targetColumn ->
                                    targetColumn.getType().equals(sourceColumn.getType()) && targetColumn.getName().equals(sourceColumn.getName()));
                    if (!matchFound) {
                        models.add(new CreatedModel(filterColumn.get(), CreatedMode.MODIFY_COLUMN));
                    }
                }
            }

            if (entity.getLifeCycleColumn() != null) {
                DataSetColumnEntity column = DataSetColumnEntity.builder()
                        .name(entity.getLifeCycleColumn())
                        .length(entity.getLifeCycle().intValue())
                        .defaultValue(entity.getLifeCycleType())
                        .build();
                models.add(new CreatedModel(column, CreatedMode.MODIFY_LIFECYCLE));
            }
        }
        catch (Exception e) {
            log.warn("Get create model for dataset [ {} ] id [ {} ] failed", entity.getName(), entity.getId(), e);
        }
        return models;
    }

    /**
     * A function to check if a table exists in the database.
     *
     * @param entity the DataSetEntity to check
     * @return true if the table exists, false otherwise
     */
    private boolean checkTableExists(DataSetEntity entity)
    {
        return pluginManager.getPlugin(initializerConfigure.getDataSetConfigure().getType())
                .map(plugin -> {
                    try {
                        PluginService pluginService = plugin.getService(PluginService.class);
                        SourceEntity source = getOutputSource();
                        String sql = String.format("SHOW CREATE TABLE `%s`.`%s`", initializerConfigure.getDataSetConfigure().getDatabase(), entity.getTableName());
                        log.info("Check table exists for dataset [ {} ] id [ {} ] sql \n {}", entity.getName(), entity.getId(), sql);
                        Response response = pluginService.execute(source.toConfigure(pluginManager, plugin), sql);
                        Preconditions.checkArgument(response.getIsSuccessful(), response.getMessage());
                        return true;
                    }
                    catch (Exception e) {
                        log.warn("Check table exists for dataset [ {} ] failed", entity.getName(), e);
                        return false;
                    }
                })
                .orElse(false);
    }

    /**
     * Retrieves the output source entity with the configured data set properties.
     *
     * @return the output source entity with the configured data set properties
     */
    private SourceEntity getOutputSource()
    {
        return SourceEntity.builder()
                .type(initializerConfigure.getDataSetConfigure().getType())
                .database(initializerConfigure.getDataSetConfigure().getDatabase())
                .host(initializerConfigure.getDataSetConfigure().getHost())
                .port(Integer.valueOf(initializerConfigure.getDataSetConfigure().getPort()))
                .username(initializerConfigure.getDataSetConfigure().getUsername())
                .password(initializerConfigure.getDataSetConfigure().getPassword())
                .protocol(PluginType.JDBC.name())
                .build();
    }

    /**
     * Formats the given CreatedModel item into a Column object.
     *
     * @param item the CreatedModel item to be formatted
     * @return the formatted Column object
     */
    private Column formatColumn(CreatedModel item)
    {
        Column column = new Column();
        column.setName(item.getColumn().getName());
        column.setType(getColumnType(item.getColumn().getType()));
        column.setComment(item.getColumn().getComment());
        column.setLength(item.getColumn().getLength());
        column.setNullable(item.getColumn().isNullable());
        column.setDefaultValue(item.getColumn().getDefaultValue());
        column.setPrimaryKey(item.getColumn().isPrimaryKey());
        return column;
    }

    /** 等到重试时间或任务被取消 / wait for the retry time or a cancel */
    private static void waitForRetry(SyncJob job, int seconds)
    {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (job.getState() == RunState.RETRYING && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(Math.min(200, Math.max(1, deadline - System.currentTimeMillis())));
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private DataSetEntity runSync(DataSetEntity entity, SyncJob job, java.util.Map<String, String> syncOverrides)
    {
        try {
            if (entity.getSource() == null || entity.getUser() == null) {
                Persistence.references().load(entity);
            }
            SourceEntity source = entity.getSource();

            pluginManager.getPlugin(source.getType())
                    .ifPresentOrElse(plugin -> {
                                PluginService inputPlugin = plugin.getService(PluginService.class);
                                pluginManager.getPlugin(entity.getExecutor())
                                        .ifPresentOrElse(
                                                executor -> {
                                                    ExecutorService executorService = executor.getService(ExecutorService.class);

                                                    Set<OriginColumn> originColumns = columnRepository.findAllByDatasetId(entity.getId())
                                                            .stream()
                                                            .filter(item -> !item.isVirtualColumn())
                                                            .map(item -> new OriginColumn(item.getName(), item.getOriginal()))
                                                            .collect(Collectors.toSet());
                                                    String database = initializerConfigure.getDataSetConfigure().getDatabase();
                                                    Properties originInputProperties = new Properties();
                                                    originInputProperties.put("driver", inputPlugin.driver());

                                                    PipelineFieldBody inputFieldBody = ConfigureUtils.convertFieldBody(
                                                            source,
                                                            entity.getExecutor(),
                                                            PluginConfigurePipelineType.INPUT,
                                                            pluginManager,
                                                            originInputProperties
                                                    );

                                                    Properties inputProperties = ConfigureUtils.convertProperties(
                                                            source, pluginManager,
                                                            PluginConfigurePipelineType.INPUT,
                                                            entity.getExecutor(),
                                                            entity.getQuery(),
                                                            inputFieldBody
                                                    );

                                                    Set<String> inputOptions = ConfigureUtils.convertOptions(
                                                            source,
                                                            pluginManager,
                                                            entity.getExecutor(),
                                                            PluginConfigurePipelineType.INPUT
                                                    );

                                                    RuntimeConfigure inputConfigure = source.toConfigure(pluginManager, plugin);
                                                    inputConfigure.setPluginManager(pluginManager);
                                                    ExecutorConfigure input = new ExecutorConfigure(
                                                            source.getType(),
                                                            inputProperties,
                                                            inputOptions,
                                                            source.getProtocol(),
                                                            inputPlugin,
                                                            entity.getQuery(),
                                                            database,
                                                            entity.getTableName(),
                                                            inputConfigure,
                                                            originColumns
                                                    );

                                                    pluginManager.getPlugin(initializerConfigure.getDataSetConfigure().getType())
                                                            .ifPresent(outPlugin -> {
                                                                PluginService outputPlugin = outPlugin.getService(PluginService.class);
                                                                SourceEntity outputSource = new SourceEntity();
                                                                outputSource.setType(initializerConfigure.getDataSetConfigure().getType());
                                                                outputSource.setDatabase(initializerConfigure.getDataSetConfigure().getDatabase());
                                                                outputSource.setHost(initializerConfigure.getDataSetConfigure().getHost());
                                                                outputSource.setPort(Integer.valueOf(initializerConfigure.getDataSetConfigure().getPort()));
                                                                outputSource.setUsername(initializerConfigure.getDataSetConfigure().getUsername());
                                                                outputSource.setPassword(initializerConfigure.getDataSetConfigure().getPassword());
                                                                outputSource.setProtocol(PluginType.JDBC.name());

                                                                Properties originOutputProperties = new Properties();
                                                                List<String> fields = Lists.newArrayList();
                                                                columnRepository.findAllByDatasetId(entity.getId())
                                                                        .forEach(item -> fields.add(item.getName()));
                                                                originOutputProperties.put("fields", String.join("\n", fields));
                                                                originOutputProperties.put("database", database);
                                                                originOutputProperties.put("table", entity.getTableName());

                                                                PipelineFieldBody outputFieldBody = ConfigureUtils.convertFieldBody(
                                                                        outputSource,
                                                                        entity.getExecutor(),
                                                                        PluginConfigurePipelineType.OUTPUT,
                                                                        pluginManager,
                                                                        originOutputProperties
                                                                );

                                                                Properties outputProperties = ConfigureUtils.convertProperties(
                                                                        outputSource,
                                                                        pluginManager,
                                                                        PluginConfigurePipelineType.OUTPUT,
                                                                        entity.getExecutor(),
                                                                        entity.getQuery(),
                                                                        outputFieldBody
                                                                );

                                                                Set<String> outputOptions = ConfigureUtils.convertOptions(
                                                                        outputSource,
                                                                        pluginManager,
                                                                        entity.getExecutor(),
                                                                        PluginConfigurePipelineType.OUTPUT
                                                                );

                                                                ExecutorConfigure output = new ExecutorConfigure(
                                                                        outputSource.getType(),
                                                                        outputProperties,
                                                                        outputOptions,
                                                                        RunProtocol.NONE.name(),
                                                                        outputPlugin,
                                                                        null,
                                                                        null,
                                                                        null,
                                                                        getConfigure(database, outPlugin),
                                                                        Sets.newHashSet()
                                                                );

                                                                // 继续或重试沿用首次运行的任务名，日志和坏行文件都在一处 / later runs keep the task name, so logs and bad rows stay together
                                                                String taskName = job.getTaskName() != null ? job.getTaskName() : DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmssSSS");
                                                                String executorKey = executor.getName().toLowerCase();
                                                                String workHome = FolderUtils.getWorkHome(
                                                                        initializerConfigure.getDataHome(),
                                                                        entity.getUser().getUsername(),
                                                                        String.join(File.separator, "dataset", "executor", executorKey, taskName)
                                                                );

                                                                // 从 metaforge_configure 取该 executor 的 effective 配置（DB 行覆盖 SPI schema 默认值）
                                                                java.util.Map<String, String> executorCfg = runtimeConfigureService.getEffective(
                                                                        RuntimeConfigureService.CATEGORY_EXECUTOR,
                                                                        executor.getName(),
                                                                        executor.runtimeConfigures()
                                                                );
                                                                // 本次同步如有用户临时覆盖：只接受 tunable=true 的字段，其余忽略
                                                                java.util.Map<String, String> overrides = syncOverrides;
                                                                if (overrides != null && !overrides.isEmpty()) {
                                                                    java.util.Set<String> tunableKeys = executor.runtimeConfigures().stream()
                                                                            .filter(org.devlive.metaforge.plugin.configure.RuntimeConfigureField::isTunable)
                                                                            .map(org.devlive.metaforge.plugin.configure.RuntimeConfigureField::getName)
                                                                            .collect(java.util.stream.Collectors.toSet());
                                                                    overrides.forEach((k, v) -> {
                                                                        if (tunableKeys.contains(k) && v != null) {
                                                                            executorCfg.put(k, v);
                                                                        }
                                                                    });
                                                                }
                                                                // 继续或重试沿用首次运行的配置，断点对应那次的分片 / later runs keep the first run's configuration, which the checkpoint belongs to
                                                                if (job.getExecutorConfigure() != null) {
                                                                    try {
                                                                        java.util.Map<String, String> saved = new com.fasterxml.jackson.databind.ObjectMapper().readValue(job.getExecutorConfigure(),
                                                                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {});
                                                                        executorCfg.clear();
                                                                        executorCfg.putAll(saved);
                                                                    }
                                                                    catch (Exception ex) {
                                                                        log.warn("Read saved executor configure failed: {}", ex.getMessage());
                                                                    }
                                                                }
                                                                // 未指定分片列时用数据集主键（其次排序键）对应的源列；仅声明了该配置的 executor 需要
                                                                if (executorCfg.containsKey("splitColumn") && StringUtils.isBlank(executorCfg.get("splitColumn"))) {
                                                                    executorCfg.put("splitColumn", defaultSplitColumn(columnRepository.findAllByDatasetId(entity.getId())));
                                                                }
                                                                // taskName / workHome 供 UI 定位日志文件；本次真正生效的配置 JSON 便于事后审计 / UI 展示
                                                                String effectiveConfigure = null;
                                                                try {
                                                                    effectiveConfigure = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(executorCfg);
                                                                }
                                                                catch (Exception ex) {
                                                                    log.warn("Serialize effective executor configure failed: {}", ex.getMessage());
                                                                }
                                                                final String executorConfigure = effectiveConfigure;
                                                                job.update(history -> {
                                                                    history.setTaskName(taskName);
                                                                    history.setWorkHome(workHome);
                                                                    history.setExecutorConfigure(executorConfigure);
                                                                });
                                                                // timeout 由各 executor 自己的配置决定：MetaForgeExecutor schema 默认 0（不限时），
                                                                // 未声明该字段的 executor（如 Seatunnel）回退到 600 秒，保持原有行为
                                                                int timeout = parseIntOrDefault(executorCfg.get("timeout"), 600);
                                                                ExecutorRequest request = new ExecutorRequest(
                                                                        taskName,
                                                                        entity.getUser().getUsername(),
                                                                        input,
                                                                        output
                                                                );
                                                                request.setWorkHome(workHome);
                                                                request.setTimeout(timeout);
                                                                request.setPluginManager(this.pluginManager);
                                                                // 把该 executor 的 effective 配置整体作为 options，各执行器各取所需：
                                                                // MetaForge 读 fetchSize/batchSize/preCount；Seatunnel 读 home/startScript/way/mode/engine
                                                                request.setOptions(executorCfg);

                                                                // 指标回调：按时间节流更新 metaforge_dataset_history 以及数据集自身的 totalRows / totalSize
                                                                // totalRows 直接用已写入计数；totalSize 必须查 ClickHouse system.parts，开销大且不可控，
                                                                // 必须放到独立线程异步执行，否则会阻塞 MySQL 流式 cursor 触发 net_write_timeout
                                                                final DataSetEntity progressEntity = entity;
                                                                final java.util.concurrent.atomic.AtomicInteger progressTick = new java.util.concurrent.atomic.AtomicInteger();
                                                                final java.util.concurrent.atomic.AtomicBoolean sizeRefreshInFlight = new java.util.concurrent.atomic.AtomicBoolean(false);
                                                                final java.util.concurrent.ExecutorService sizeRefreshExecutor = Executors.newSingleThreadExecutor(r -> {
                                                                    Thread t = new Thread(r, "dataset-size-refresh-" + taskName);
                                                                    t.setDaemon(true);
                                                                    return t;
                                                                });
                                                                request.setMetricsListener(metrics -> {
                                                                    if (!syncJobManager.onMetrics(job, metrics, false)) {
                                                                        return;
                                                                    }
                                                                    // 同步期间用已写入行数滚动刷新 dataset.totalRows，方便列表实时看到进度
                                                                    try {
                                                                        progressEntity.setTotalRows(metrics.getRowsWritten());
                                                                        repository.save(progressEntity);
                                                                    }
                                                                    catch (Exception ex) {
                                                                        log.warn("Update dataset totalRows failed: {}", ex.getMessage());
                                                                    }
                                                                    // 节流 + 异步刷新 totalSize：每 SIZE_REFRESH_EVERY 次落库提交一次到独立线程；
                                                                    // 上一次还没回来就跳过，绝不阻塞主流（MySQL 真流式 cursor 对客户端读速度敏感）
                                                                    if (progressTick.incrementAndGet() % SIZE_REFRESH_EVERY == 0
                                                                            && sizeRefreshInFlight.compareAndSet(false, true)) {
                                                                        sizeRefreshExecutor.submit(() -> {
                                                                            try {
                                                                                this.flushTableMetadata(
                                                                                        progressEntity,
                                                                                        outputPlugin,
                                                                                        database,
                                                                                        requireNonNull(output.getOriginConfigure()),
                                                                                        outPlugin
                                                                                );
                                                                            }
                                                                            catch (Exception ex) {
                                                                                log.warn("Refresh dataset totalSize failed: {}", ex.getMessage());
                                                                            }
                                                                            finally {
                                                                                sizeRefreshInFlight.set(false);
                                                                            }
                                                                        });
                                                                    }
                                                                });

                                                                request.setCheckpoint(job.getCheckpoint());
                                                                int maxRetries = parseIntOrDefault(executorCfg.get("maxRetries"), 0);
                                                                int retryInterval = parseIntOrDefault(executorCfg.get("retryIntervalSeconds"), 30);
                                                                ExecutorResponse response;
                                                                try {
                                                                    int retries = 0;
                                                                    while (true) {
                                                                        response = executorService.start(request);
                                                                        final String checkpoint = response.getCheckpoint();
                                                                        job.update(history -> history.setCheckpoint(checkpoint));
                                                                        if (response.getState() != RunState.FAILURE || !response.getRetryable() || retries >= maxRetries) {
                                                                            break;
                                                                        }
                                                                        retries++;
                                                                        if (response.getMetrics() != null) {
                                                                            syncJobManager.onMetrics(job, response.getMetrics(), true);
                                                                        }
                                                                        if (!job.transition(RunState.RETRYING, response.getMessage())) {
                                                                            break;
                                                                        }
                                                                        waitForRetry(job, retryInterval);
                                                                        // 等待期间被取消时转换会被拒绝 / a cancel during the wait makes this move illegal
                                                                        if (!job.transition(RunState.RUNNING, null)) {
                                                                            return;
                                                                        }
                                                                        job.update(history -> history.setAttempt(history.getAttempt() == null ? 2 : history.getAttempt() + 1));
                                                                        request.setCheckpoint(checkpoint);
                                                                    }
                                                                }
                                                                finally {
                                                                    // 关闭异步 size 刷新线程，等正在跑的最多 30 秒
                                                                    sizeRefreshExecutor.shutdown();
                                                                    try {
                                                                        if (!sizeRefreshExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                                                                            sizeRefreshExecutor.shutdownNow();
                                                                        }
                                                                    }
                                                                    catch (InterruptedException ie) {
                                                                        sizeRefreshExecutor.shutdownNow();
                                                                        Thread.currentThread().interrupt();
                                                                    }
                                                                }
                                                                if (response.getMetrics() != null) {
                                                                    syncJobManager.onMetrics(job, response.getMetrics(), true);
                                                                }
                                                                final ExecutorResponse finished = response;
                                                                job.update(history -> {
                                                                    history.setCount(finished.getCount());
                                                                    if (finished.getSuccessful()) {
                                                                        history.setProcessedCount((long) finished.getCount());
                                                                        if (history.getTotalCount() == null || history.getTotalCount() < 0L) {
                                                                            history.setTotalCount((long) finished.getCount());
                                                                        }
                                                                        history.setProgress(java.math.BigDecimal.valueOf(100.0).setScale(2, java.math.RoundingMode.HALF_UP));
                                                                    }
                                                                });
                                                                RunState finalState = response.getState() != null
                                                                        ? response.getState()
                                                                        : (response.getSuccessful() ? RunState.SUCCESS : RunState.FAILURE);
                                                                // 只有成功才刷新表元数据，刷新完成后才算成功；STOPPED / TIMEOUT / FAILURE 直接落终态
                                                                if (finalState == RunState.SUCCESS) {
                                                                    long finalizeStart = System.currentTimeMillis();
                                                                    syncJobManager.stage(job, SYNC_STAGE_FINALIZE, "RUNNING", 0);
                                                                    try {
                                                                        this.flushTableMetadata(
                                                                                entity,
                                                                                outputPlugin,
                                                                                database,
                                                                                requireNonNull(output.getOriginConfigure()),
                                                                                outPlugin
                                                                        );
                                                                    }
                                                                    catch (RuntimeException ex) {
                                                                        syncJobManager.stage(job, SYNC_STAGE_FINALIZE, "FAILED", System.currentTimeMillis() - finalizeStart);
                                                                        throw ex;
                                                                    }
                                                                    syncJobManager.stage(job, SYNC_STAGE_FINALIZE, "DONE", System.currentTimeMillis() - finalizeStart);
                                                                }
                                                                job.transition(finalState, response.getMessage());
                                                            });
                                                },
                                                () -> {
                                                    log.error("Executor [ {} ] not found", entity.getExecutor());
                                                    job.transition(RunState.FAILURE, String.format("Executor [ %s ] not found", entity.getExecutor()));
                                                }
                                        );
                            },
                            () -> {
                                throw new IllegalArgumentException(String.format("Plugin [ %s ] not found", initializerConfigure.getDataSetConfigure().getType()));
                            });
        }
        catch (Exception e) {
            log.warn("Sync data for dataset [ {} ] failed", entity.getName(), e);
            job.transition(RunState.FAILURE, e.getMessage());
        }
        return entity;
    }
}
