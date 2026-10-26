package org.devlive.metaforge.service.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.devlive.metaforge.common.enums.DataSetState;
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
import org.devlive.metaforge.executor.configure.ExecutorConfigure;
import org.devlive.metaforge.executor.configure.ExecutorRequest;
import org.devlive.metaforge.executor.configure.ExecutorResponse;
import org.devlive.metaforge.executor.configure.OriginColumn;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.scheduler.SchedulerRequest;
import org.devlive.metaforge.scheduler.SchedulerService;
import org.devlive.metaforge.service.adapter.PageRequestAdapter;
import org.devlive.metaforge.service.annotation.SendNotification;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.PipelineFieldBody;
import org.devlive.metaforge.service.body.adhoc.Adhoc;
import org.devlive.metaforge.service.common.ConfigureUtils;
import org.devlive.metaforge.service.common.FolderUtils;
import org.devlive.metaforge.service.configure.IConfigurePipelineType;
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
import org.devlive.metaforge.service.enums.NotificationType;
import org.devlive.metaforge.service.enums.QueryMode;
import org.devlive.metaforge.service.enums.SyncMode;
import org.devlive.metaforge.service.event.NotificationEventPublisher;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.initializer.job.DatasetJob;
import org.devlive.metaforge.service.model.CreatedModel;
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
import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Response;
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

import javax.transaction.Transactional;

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
    // 同步期间每 SIZE_REFRESH_EVERY 次进度回调才刷一次 totalSize（查 ClickHouse system.parts 较重）
    private static final int SIZE_REFRESH_EVERY = 10;

    // 已请求停止但还没真正停下来的 history id 集合：进度回调据此把 state 维持为 STOPPING 而不是 RUNNING
    // Set of history ids whose sync has been asked to stop but hasn't reached its cancellation checkpoint yet.
    private static final java.util.Set<Long> stoppingHistoryIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // 本次同步的 tunable 字段临时覆盖。由 syncData(code, overrides) 在异步线程入口写入，由内部 sync 体读取后清理。
    // Per-invocation tunable overrides; set by the public syncData entry, read inside the async body.
    private static final ThreadLocal<java.util.Map<String, String>> currentSyncOverrides = new ThreadLocal<>();

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

    public DataSetServiceImpl(DataSetRepository repository, DataSetColumnRepository columnRepository, DatasetHistoryRepository historyRepository, PluginManager pluginManager, InitializerConfigure initializerConfigure, org.quartz.Scheduler scheduler, Environment environment, SourceRepository sourceRepository, NotificationRepository notificationRepository, NotificationEventPublisher eventPublisher, RuntimeConfigureService runtimeConfigureService)
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
    }

    @Transactional
    @Override
    @SendNotification(type = NotificationType.DYNAMIC)
    public CommonResponse<DataSetEntity> saveOrUpdate(BaseRepository<DataSetEntity, Long> repository, DataSetEntity configure)
    {
        UserEntity user = UserDetailsService.getUser();
        java.util.concurrent.ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            configure.setUser(user);
            completeState(configure, DataSetState.METADATA_START);
            startBuild(configure, true);
        });
        return CommonResponse.success(configure);
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
                    return CommonResponse.success(columnRepository.findAllByDatasetOrderByPositionAsc(item));
                })
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
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

                    SqlBody body = SqlBody.builder()
                            .type(SqlType.SELECT)
                            .database(database)
                            .table(item.getTableName())
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
                .map(CommonResponse::success)
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<PageEntity<DatasetHistoryEntity>> getHistory(String code, FilterBody filter)
    {
        return repository.findByCode(code)
                .map(item -> {
                    Pageable pageable = PageRequestAdapter.of(filter);
                    return CommonResponse.success(PageEntity.build(historyRepository.findAllByDatasetOrderByCreateTimeDesc(item, pageable)));
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
        return historyRepository.findById(id)
                .map(history -> {
                    log.info("Stop history [ {} ] state={} taskName={}", id, history.getState(), history.getTaskName());
                    if (history.getState() != RunState.RUNNING && history.getState() != RunState.CREATED) {
                        return CommonResponse.<Boolean>failure(String.format("Sync history [ %d ] is not running (current state: %s)", id, history.getState()));
                    }
                    String taskName = history.getTaskName();
                    if (StringUtils.isBlank(taskName)) {
                        return CommonResponse.<Boolean>failure("Sync history has no taskName (older record), cannot stop");
                    }
                    DataSetEntity dataset = history.getDataset();
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
                        // 让 UI 立刻看到 STOPPING 中间态；进度回调会继续把它保持住直到真正停下
                        stoppingHistoryIds.add(id);
                        history.setState(RunState.STOPPING);
                        historyRepository.save(history);
                        return CommonResponse.success(Boolean.TRUE);
                    }
                    return CommonResponse.<Boolean>failure(stopResponse.getMessage() == null ? "Stop failed" : stopResponse.getMessage());
                })
                .orElseGet(() -> CommonResponse.<Boolean>failure(String.format("Sync history [ %d ] not found", id)));
    }

    @Override
    @Transactional
    @SendNotification(type = NotificationType.DELETED)
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
                        repository.delete(value);
                    });
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

    private Configure getConfigure(String database, Plugin plugin)
    {
        return Configure.builder()
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
        sourceRepository.findByCode(entity.getSource().getCode())
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

                List<DataSetColumnEntity> originalColumns = columnRepository.findAllByDataset(entity);
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
                                List<DataSetColumnEntity> columnEntities = columnRepository.findAllByDataset(entity);
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

                                Configure targetConfigure = new Configure();
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

    private PluginService getOutputPlugin()
    {
        return pluginManager.getPlugin(initializerConfigure.getDataSetConfigure().getType())
                .map(plugin -> plugin.getService(PluginService.class))
                .orElseThrow(() -> new RuntimeException("Not found service from %s" + initializerConfigure.getDataSetConfigure().getType()));
    }

    @Override
    public CommonResponse<DataSetEntity> syncData(String code)
    {
        return syncData(code, null);
    }

    @Override
    public CommonResponse<DataSetEntity> syncData(String code, java.util.Map<String, String> overrides)
    {
        return repository.findByCode(code)
                .map(entity -> {
                    UserEntity currentUser = UserDetailsService.getUser();
                    java.util.concurrent.ExecutorService service = Executors.newSingleThreadExecutor();
                    java.util.Map<String, String> safeOverrides = overrides == null
                            ? java.util.Collections.emptyMap()
                            : new java.util.HashMap<>(overrides);

                    service.submit(() -> {
                        // 在异步线程中设置用户上下文
                        setAuthenticationContext(currentUser);
                        currentSyncOverrides.set(safeOverrides);
                        try {
                            DataSetEntity result = syncData(entity, service);
                            eventPublisher.publishNotificationEvent(
                                    result,
                                    null,
                                    "",
                                    EntityType.DATASET,
                                    NotificationType.SYNCDATA,
                                    new String[] {}
                            );
                        }
                        finally {
                            currentSyncOverrides.remove();
                        }
                    });
                    return CommonResponse.success(entity);
                })
                .orElseGet(() -> CommonResponse.failure(String.format("DataSet [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<java.util.List<org.devlive.metaforge.plugin.configure.PluginConfigureField>> getSyncFields(String code)
    {
        return repository.findByCode(code)
                .<CommonResponse<java.util.List<org.devlive.metaforge.plugin.configure.PluginConfigureField>>>map(entity -> {
                    return pluginManager.getPlugin(entity.getExecutor())
                            .map(plugin -> {
                                java.util.List<org.devlive.metaforge.plugin.configure.PluginConfigureField> schema = plugin.configures();
                                java.util.Map<String, String> effective = runtimeConfigureService.getEffective(
                                        RuntimeConfigureService.CATEGORY_EXECUTOR,
                                        plugin.getName(),
                                        schema
                                );
                                java.util.List<org.devlive.metaforge.plugin.configure.PluginConfigureField> tunable = schema.stream()
                                        .filter(org.devlive.metaforge.plugin.configure.PluginConfigureField::isTunable)
                                        .map(f -> new org.devlive.metaforge.plugin.configure.PluginConfigureField(
                                                f.getName(),
                                                f.getType(),
                                                effective.getOrDefault(f.getName(), f.getDefaultValue()),
                                                f.getDescription(),
                                                true
                                        ))
                                        .collect(java.util.stream.Collectors.toList());
                                return CommonResponse.success(tunable);
                            })
                            .orElseGet(() -> CommonResponse.<java.util.List<org.devlive.metaforge.plugin.configure.PluginConfigureField>>failure(
                                    String.format("Executor [ %s ] not found", entity.getExecutor())));
                })
                .orElseGet(() -> CommonResponse.<java.util.List<org.devlive.metaforge.plugin.configure.PluginConfigureField>>failure(
                        String.format("DataSet [ %s ] not found", code)));
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
    private void flushTableMetadata(DataSetEntity entity, PluginService pluginService, String database, Configure configure, Plugin plugin)
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
                    entity.setTotalRows(data.get(0).toString());
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

            List<DataSetColumnEntity> sourceColumns = columnRepository.findAllByDataset(entity)
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
                        .length(Integer.parseInt(entity.getLifeCycle()))
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

    private DataSetEntity syncData(DataSetEntity entity, java.util.concurrent.ExecutorService service)
    {
        DatasetHistoryEntity history = new DatasetHistoryEntity();
        try {
            SourceEntity source = entity.getSource();

            pluginManager.getPlugin(source.getType())
                    .ifPresentOrElse(plugin -> {
                                history.setState(RunState.CREATED);
                                history.setCreateTime(new Date());
                                history.setQuery(entity.getQuery());
                                history.setDataset(entity);
                                historyRepository.save(history);

                                PluginService inputPlugin = plugin.getService(PluginService.class);
                                pluginManager.getPlugin(entity.getExecutor())
                                        .ifPresentOrElse(
                                                executor -> {
                                                    ExecutorService executorService = executor.getService(ExecutorService.class);

                                                    Set<OriginColumn> originColumns = columnRepository.findAllByDataset(entity)
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
                                                            IConfigurePipelineType.INPUT,
                                                            environment,
                                                            originInputProperties
                                                    );

                                                    Properties inputProperties = ConfigureUtils.convertProperties(
                                                            source, environment,
                                                            IConfigurePipelineType.INPUT,
                                                            entity.getExecutor(),
                                                            entity.getQuery(),
                                                            inputFieldBody
                                                    );

                                                    Set<String> inputOptions = ConfigureUtils.convertOptions(
                                                            source,
                                                            environment,
                                                            entity.getExecutor(),
                                                            IConfigurePipelineType.INPUT
                                                    );

                                                    Configure inputConfigure = source.toConfigure(pluginManager, plugin);
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
                                                                columnRepository.findAllByDataset(entity)
                                                                        .forEach(item -> fields.add(item.getName()));
                                                                originOutputProperties.put("fields", String.join("\n", fields));
                                                                originOutputProperties.put("database", database);
                                                                originOutputProperties.put("table", entity.getTableName());

                                                                PipelineFieldBody outputFieldBody = ConfigureUtils.convertFieldBody(
                                                                        outputSource,
                                                                        entity.getExecutor(),
                                                                        IConfigurePipelineType.OUTPUT,
                                                                        environment,
                                                                        originOutputProperties
                                                                );

                                                                Properties outputProperties = ConfigureUtils.convertProperties(
                                                                        outputSource,
                                                                        environment,
                                                                        IConfigurePipelineType.OUTPUT,
                                                                        entity.getExecutor(),
                                                                        entity.getQuery(),
                                                                        outputFieldBody
                                                                );

                                                                Set<String> outputOptions = ConfigureUtils.convertOptions(
                                                                        outputSource,
                                                                        environment,
                                                                        entity.getExecutor(),
                                                                        IConfigurePipelineType.OUTPUT
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

                                                                String taskName = DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmssSSS");
                                                                String executorKey = executor.getName().toLowerCase();
                                                                String workHome = FolderUtils.getWorkHome(
                                                                        initializerConfigure.getDataHome(),
                                                                        entity.getUser().getUsername(),
                                                                        String.join(File.separator, "dataset", "executor", executorKey, taskName)
                                                                );
                                                                // 把 taskName / workHome 落到 history，供 UI 定位日志文件
                                                                history.setTaskName(taskName);
                                                                history.setWorkHome(workHome);

                                                                // 从 metaforge_configure 取该 executor 的 effective 配置（DB 行覆盖 SPI schema 默认值）
                                                                java.util.Map<String, String> executorCfg = runtimeConfigureService.getEffective(
                                                                        RuntimeConfigureService.CATEGORY_EXECUTOR,
                                                                        executor.getName(),
                                                                        executor.configures()
                                                                );
                                                                // 本次同步如有用户临时覆盖：只接受 tunable=true 的字段，其余忽略
                                                                java.util.Map<String, String> overrides = currentSyncOverrides.get();
                                                                if (overrides != null && !overrides.isEmpty()) {
                                                                    java.util.Set<String> tunableKeys = executor.configures().stream()
                                                                            .filter(org.devlive.metaforge.plugin.configure.PluginConfigureField::isTunable)
                                                                            .map(org.devlive.metaforge.plugin.configure.PluginConfigureField::getName)
                                                                            .collect(java.util.stream.Collectors.toSet());
                                                                    overrides.forEach((k, v) -> {
                                                                        if (tunableKeys.contains(k) && v != null) {
                                                                            executorCfg.put(k, v);
                                                                        }
                                                                    });
                                                                }
                                                                // 把本次同步真正生效的配置 JSON 落到 history，便于事后审计 / UI 展示
                                                                try {
                                                                    history.setExecutorConfigure(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(executorCfg));
                                                                }
                                                                catch (Exception ex) {
                                                                    log.warn("Serialize effective executor configure failed: {}", ex.getMessage());
                                                                }
                                                                historyRepository.save(history);
                                                                // timeout 由各 executor 自己的配置决定：LocalExecutor schema 默认 0（不限时），
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
                                                                // Local 读 fetchSize/batchSize/preCount；Seatunnel 读 home/startScript/way/mode/engine
                                                                request.setOptions(executorCfg);

                                                                // 进度回调：每个 batch 写完后更新 metaforge_dataset_history 以及数据集自身的 totalRows / totalSize
                                                                // totalRows 直接用已写入计数；totalSize 必须查 ClickHouse system.parts，开销大且不可控，
                                                                // 必须放到独立线程异步执行，否则会阻塞 MySQL 流式 cursor 触发 net_write_timeout
                                                                final DatasetHistoryEntity progressHistory = history;
                                                                final DataSetEntity progressEntity = entity;
                                                                final java.util.concurrent.atomic.AtomicInteger progressTick = new java.util.concurrent.atomic.AtomicInteger();
                                                                final java.util.concurrent.atomic.AtomicBoolean sizeRefreshInFlight = new java.util.concurrent.atomic.AtomicBoolean(false);
                                                                final java.util.concurrent.ExecutorService sizeRefreshExecutor = Executors.newSingleThreadExecutor(r -> {
                                                                    Thread t = new Thread(r, "dataset-size-refresh-" + taskName);
                                                                    t.setDaemon(true);
                                                                    return t;
                                                                });
                                                                request.setProgressListener((processed, total) -> {
                                                                    progressHistory.setProcessedCount(processed);
                                                                    if (total >= 0) {
                                                                        progressHistory.setTotalCount(total);
                                                                        if (total > 0) {
                                                                            java.math.BigDecimal percent = java.math.BigDecimal
                                                                                    .valueOf(processed * 100.0 / total)
                                                                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                                                                            progressHistory.setProgress(percent);
                                                                        }
                                                                    }
                                                                    // 收到停止请求后，进度回调不能把 state 写回 RUNNING；保持 STOPPING 直到真正退出
                                                                    if (stoppingHistoryIds.contains(progressHistory.getId())) {
                                                                        progressHistory.setState(RunState.STOPPING);
                                                                    }
                                                                    try {
                                                                        historyRepository.save(progressHistory);
                                                                    }
                                                                    catch (Exception ex) {
                                                                        log.warn("Update sync history progress failed: {}", ex.getMessage());
                                                                    }
                                                                    // 同步期间用已写入行数滚动刷新 dataset.totalRows，方便列表实时看到进度
                                                                    try {
                                                                        progressEntity.setTotalRows(String.valueOf(processed));
                                                                        repository.save(progressEntity);
                                                                    }
                                                                    catch (Exception ex) {
                                                                        log.warn("Update dataset totalRows failed: {}", ex.getMessage());
                                                                    }
                                                                    // 节流 + 异步刷新 totalSize：每 SIZE_REFRESH_EVERY 次 progress 提交一次到独立线程；
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

                                                                history.setState(RunState.RUNNING);
                                                                historyRepository.save(history);

                                                                ExecutorResponse response;
                                                                try {
                                                                    response = executorService.start(request);
                                                                }
                                                                finally {
                                                                    // 任务一旦真正退出，从 STOPPING 集合移除（不管是正常 / 失败 / 被停止）
                                                                    stoppingHistoryIds.remove(progressHistory.getId());
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
                                                                history.setUpdateTime(new Date());
                                                                history.setElapsed((history.getUpdateTime().getTime() - history.getCreateTime().getTime()) / 1000);
                                                                history.setMode(QueryMode.SYNC);
                                                                history.setCount(response.getCount());
                                                                history.setState(response.getState());
                                                                if (response.getMessage() != null) {
                                                                    history.setMessage(response.getMessage());
                                                                }
                                                                if (response.getSuccessful()) {
                                                                    history.setProcessedCount((long) response.getCount());
                                                                    if (history.getTotalCount() == null || history.getTotalCount() < 0L) {
                                                                        history.setTotalCount((long) response.getCount());
                                                                    }
                                                                    history.setProgress(java.math.BigDecimal.valueOf(100.0).setScale(2, java.math.RoundingMode.HALF_UP));
                                                                }
                                                                historyRepository.save(history);
                                                                // STOPPED（用户主动停止）与 TIMEOUT（超时取消）都是终态：已把最终状态写入 history，
                                                                // 无需当作异常抛出 / 也不刷 table metadata
                                                                if (response.getState() == RunState.STOPPED || response.getState() == RunState.TIMEOUT) {
                                                                    return;
                                                                }
                                                                Preconditions.checkArgument(response.getSuccessful(), response.getMessage());

                                                                this.flushTableMetadata(
                                                                        entity,
                                                                        outputPlugin,
                                                                        database,
                                                                        requireNonNull(output.getOriginConfigure()),
                                                                        outPlugin
                                                                );
                                                            });
                                                },
                                                () -> {
                                                    log.error("Executor [ {} ] not found", entity.getExecutor());
                                                    history.setMessage(String.format("Executor [ %s ] not found", entity.getExecutor()));
                                                    history.setState(RunState.FAILURE);
                                                    historyRepository.save(history);
                                                }
                                        );
                            },
                            () -> {
                                throw new IllegalArgumentException(String.format("Plugin [ %s ] not found", initializerConfigure.getDataSetConfigure().getType()));
                            });
        }
        catch (Exception e) {
            log.warn("Sync data for dataset [ {} ] failed", entity.getName(), e);
            history.setState(RunState.FAILURE);
            history.setMessage(e.getMessage());
        }
        finally {
            historyRepository.save(history);
            service.shutdownNow();
        }
        return entity;
    }
}
