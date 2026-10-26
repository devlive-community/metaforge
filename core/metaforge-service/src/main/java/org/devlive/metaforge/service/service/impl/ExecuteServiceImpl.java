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
import org.devlive.metaforge.service.entity.ExecuteEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.ExecuteService;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class ExecuteServiceImpl
        implements ExecuteService
{
    private final SourceRepository sourceRepository;
    private final Environment environment;
    private final PluginManager pluginManager;

    public ExecuteServiceImpl(SourceRepository sourceRepository, Environment environment, PluginManager pluginManager)
    {
        this.sourceRepository = sourceRepository;
        this.environment = environment;
        this.pluginManager = pluginManager;
    }

    @AuditPlugin
    @Override
    public CommonResponse<Object> execute(ExecuteEntity configure)
    {
        return sourceRepository.findByCode(configure.getName())
                .map(entity -> handleSourceEntity(entity, configure.getContent()))
                .orElse(CommonResponse.failure(ServiceState.SOURCE_NOT_FOUND));
    }

    /**
     * 处理源实体
     * Handle source entity
     *
     * @param entity {@link SourceEntity} 源实体配置 / Source entity configuration
     * @param content SQL语句 / SQL statement
     * @return 插件执行结果 / Plugin execution result
     */
    private CommonResponse<Object> handleSourceEntity(SourceEntity entity, String content)
    {
        return pluginManager.getPlugin(entity.getType())
                .map(plugin -> executeWithPlugin(entity, plugin, content))
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
    private CommonResponse<Object> executeWithPlugin(SourceEntity entity, Plugin plugin, String content)
    {
        try {
            PluginService service = plugin.getService(PluginService.class);
            configureEntity(entity);

            // 执行插件并获取响应
            // Execute the plugin and get the response
            Response response = executePlugin(service, entity, plugin, content);

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
    private Response executePlugin(PluginService service, SourceEntity entity, Plugin plugin, String content)
    {
        Response response = service.execute(
                entity.toConfigure(pluginManager, plugin),
                content
        );
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
