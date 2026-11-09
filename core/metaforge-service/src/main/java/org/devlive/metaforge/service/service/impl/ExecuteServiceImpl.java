package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.sql.SqlBuilder;
import org.devlive.metaforge.common.sql.configure.SqlBody;
import org.devlive.metaforge.common.sql.configure.SqlType;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.audit.AuditPlugin;
import org.devlive.metaforge.service.body.ExecuteDslBody;
import org.devlive.metaforge.service.configure.QuerySchema;
import org.devlive.metaforge.service.entity.ExecuteEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.enums.QueryMode;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.ExecuteService;
import org.devlive.metaforge.service.service.RuntimeConfigureService;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.sql.SqlLimit;
import org.devlive.metaforge.spi.sql.TableScopedSql;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class ExecuteServiceImpl
        implements ExecuteService
{
    /**
     * Engines accepting a plain {@code EXPLAIN} prefix, matched on the normalized
     * (lowercase, alphanumeric only) plugin type.
     */
    private static final Set<String> PLAN_DIRECT_ENGINES = Set.of(
            "mysql", "doris", "starrocks", "oceanbase", "postgresql", "timescaledb",
            "paradedb", "trino", "presto", "clickhouse", "hive", "hive2x", "impala",
            "duckdb", "kylin", "kyuubi", "greptimedb", "ceresdb", "matrixone",
            "hologres", "ignite", "dremio");

    /** Engines whose dialect ends a query with {@code LIMIT n}, so the row cap can be written into the SQL. */
    private static final Set<String> LIMIT_ENGINES = Set.of(
            "mysql", "mariadb", "doris", "starrocks", "oceanbase", "tidb", "postgresql", "timescaledb",
            "paradedb", "greenplum", "trino", "presto", "clickhouse", "hive", "hive2x", "impala", "kyuubi",
            "duckdb", "sqlite", "h2", "h2tcp", "greptimedb", "ceresdb", "matrixone", "hologres", "questdb",
            "cratedb", "databend", "snowflake");

    /** Engines requiring the {@code EXPLAIN PLAN FOR} statement instead. */
    private static final Set<String> PLAN_FOR_ENGINES = Set.of("h2", "h2tcp");

    private final SourceRepository sourceRepository;
    private final Environment environment;
    private final PluginManager pluginManager;
    private final RuntimeConfigureService runtimeConfigureService;

    public ExecuteServiceImpl(SourceRepository sourceRepository, Environment environment, PluginManager pluginManager, RuntimeConfigureService runtimeConfigureService)
    {
        this.sourceRepository = sourceRepository;
        this.environment = environment;
        this.pluginManager = pluginManager;
        this.runtimeConfigureService = runtimeConfigureService;
    }

    @AuditPlugin
    @Override
    public CommonResponse<Object> execute(ExecuteEntity configure)
    {
        String database = null;
        if (StringUtils.isNotBlank(configure.getTable())) {
            Optional<String> violation = TableScopedSql.validate(configure.getContent(), configure.getDatabase(), configure.getTable());
            if (violation.isPresent()) {
                return CommonResponse.failure(ServiceState.PLUGIN_EXECUTE_FAILED, violation.get());
            }
            database = StringUtils.trimToNull(configure.getDatabase());
        }
        String scopedDatabase = database;
        int maxRows = maxRows(configure.getMode());
        return sourceRepository.findByCode(configure.getName())
                .map(entity -> handleSourceEntity(entity, configure.getContent(), scopedDatabase, maxRows))
                .orElse(CommonResponse.failure(ServiceState.SOURCE_NOT_FOUND));
    }

    @AuditPlugin
    @Override
    public CommonResponse<Object> plan(ExecuteEntity configure)
    {
        return sourceRepository.findByCode(configure.getName())
                .map(entity -> handlePlanSourceEntity(entity, configure.getContent()))
                .orElse(CommonResponse.failure(ServiceState.SOURCE_NOT_FOUND));
    }

    /** The source type as an engine key: lower case, letters and digits only. */
    private static String engine(SourceEntity entity)
    {
        return StringUtils.defaultString(entity.getType()).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * 查询页（即席查询、历史、片段）的结果行数上限，其他调用方（报表、数据集、同步）不限制
     * Row cap for statements run from the query page (ad hoc, history, snippet); reports, datasets
     * and syncs read their results in full
     */
    private int maxRows(QueryMode mode)
    {
        if (mode != null && mode != QueryMode.ADHOC && mode != QueryMode.HISTORY && mode != QueryMode.SNIPPET) {
            return 0;
        }
        return QuerySchema.maxRows(runtimeConfigureService
                .getEffective(RuntimeConfigureService.CATEGORY_QUERY, QuerySchema.NAME, QuerySchema.fields())
                .get(QuerySchema.MAX_ROWS));
    }

    /**
     * 处理查询计划请求，把 SQL 包装成当前引擎支持的 EXPLAIN 语句
     * Handle a query plan request by wrapping the SQL into the EXPLAIN
     * statement of the referenced engine
     *
     * @param entity {@link SourceEntity} 源实体配置 / Source entity configuration
     * @param content SQL语句 / SQL statement
     * @return 插件执行结果 / Plugin execution result
     */
    private CommonResponse<Object> handlePlanSourceEntity(SourceEntity entity, String content)
    {
        String type = engine(entity);
        String prefix;
        if (PLAN_DIRECT_ENGINES.contains(type)) {
            prefix = "EXPLAIN ";
        }
        else if (PLAN_FOR_ENGINES.contains(type)) {
            prefix = "EXPLAIN PLAN FOR ";
        }
        else {
            return CommonResponse.failure(ServiceState.PLUGIN_EXECUTE_FAILED, "Engine [" + entity.getType() + "] does not support query plan");
        }
        String statement = StringUtils.stripEnd(StringUtils.trimToEmpty(content), "; \n\r\t");
        if (StringUtils.isEmpty(statement)) {
            return CommonResponse.failure(ServiceState.PLUGIN_EXECUTE_FAILED, "Query plan requires a SQL statement");
        }
        return handleSourceEntity(entity, prefix + statement, null, 0);
    }

    /**
     * 处理源实体
     * Handle source entity
     *
     * @param entity {@link SourceEntity} 源实体配置 / Source entity configuration
     * @param content SQL语句 / SQL statement
     * @return 插件执行结果 / Plugin execution result
     */
    private CommonResponse<Object> handleSourceEntity(SourceEntity entity, String content, String database, int maxRows)
    {
        return pluginManager.getPlugin(entity.getType())
                .map(plugin -> executeWithPlugin(entity, plugin, content, database, maxRows))
                .orElse(CommonResponse.failure(ServiceState.PLUGIN_NOT_FOUND));
    }

    /**
     * 执行插件并获取响应
     * Execute the plugin and get the RESPONSE
     *
     * @param entity {@link SourceEntity} 源实体配置 / Source entity configuration
     * @param plugin {@link Plugin} 目标插件 / Target plugin
     * @param content SQL语句 / SQL statement
     * @return 插件执行结果 / Plugin execution result
     */
    private CommonResponse<Object> executeWithPlugin(SourceEntity entity, Plugin plugin, String content, String database, int maxRows)
    {
        try {
            PluginService service = plugin.getService(PluginService.class);
            configureEntity(entity);

            // 执行插件并获取响应
            // Execute the plugin and get the response
            Response response = executePlugin(service, entity, plugin, content, database, maxRows);

            if (response.getIsSuccessful()) {
                return CommonResponse.success(response);
            }
            return CommonResponse.failure(ServiceState.PLUGIN_EXECUTE_FAILED, response.getMessage());
        }
        catch (Exception e) {
            log.error("Failed to execute plugin for entity: {}", entity.getId(), e);
            return CommonResponse.failure(ServiceState.PLUGIN_EXECUTE_FAILED, "Plugin execution failed: " + e.getMessage());
        }
    }

    /**
     * 设置用户自定义配置
     * Set user-defined configuration
     *
     * @param entity {@link SourceEntity} 源实体配置 / Source entity configuration
     */
    private void configureEntity(SourceEntity entity)
    {
        if (entity.isUsedConfig()) {
            if (entity.getUser() == null) {
                Persistence.references().load(entity);
            }
            entity.setUsername(entity.getUser().getUsername());
            String configHome = getConfigHome();
            entity.setHome(configHome);
        }
    }

    /**
     * 获取配置目录
     * Get configuration directory
     *
     * @return 配置目录 / Configuration directory
     */
    private String getConfigHome()
    {
        String configHome = environment.getProperty("metaforge.config.data");
        if (StringUtils.isEmpty(configHome)) {
            configHome = String.join(File.separator, System.getProperty("user.dir"), "config");
        }
        return configHome;
    }

    /**
     * 向插件发布任务并执行操作
     * Publish task to plugin and execute operation
     *
     * @param service {@link PluginService} 插件服务实例 / Plugin service instance
     * @param entity {@link SourceEntity} 源实体配置 / Source entity configuration
     * @param plugin {@link Plugin} 目标插件 / Target plugin
     * @param content {@link String} 执行内容 / Execution content
     * @return {@link Response} 插件执行结果 / Plugin execution result
     */
    private Response executePlugin(PluginService service, SourceEntity entity, Plugin plugin, String content, String database, int maxRows)
    {
        RuntimeConfigure runtime = entity.toConfigure(pluginManager, plugin);
        runtime.setMaxRows(maxRows);
        String statement = content;
        if (maxRows > 0 && LIMIT_ENGINES.contains(engine(entity))) {
            // One row past the cap tells the adapter the result was longer; the database reads no further.
            statement = SqlLimit.append(content, maxRows + 1).orElse(content);
        }
        if (database != null) {
            // A table-scoped statement runs in the table's database so unqualified names resolve to it.
            runtime.setDatabase(Optional.of(database));
        }
        Response response = service.execute(runtime, statement);
        // History and the editor keep the statement as the user wrote it.
        response.setContent(content);
        return response;
    }

    @Override
    public CommonResponse<Object> execute(ExecuteDslBody configure)
    {
        ExecuteEntity executeEntity = new ExecuteEntity();
        executeEntity.setEnv(configure.getEnv());
        executeEntity.setName(configure.getName());
        executeEntity.setFormat(configure.getFormat());
        SqlBody body = configure.getConfigure();
        body.setType(SqlType.SELECT);
        SqlBuilder builder = new SqlBuilder(body);
        executeEntity.setContent(builder.getSql());
        return this.execute(executeEntity);
    }
}
