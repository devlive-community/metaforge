package org.devlive.metaforge.service.source;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.schedule.ScheduledRunnable;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * 数据源健康检查定时任务运行器
 * Scheduled task runner for checking data source health status
 *
 * <p>
 * 负责定期检查所有数据源的连接状态、版本信息等，并更新数据源状态
 * This class is responsible for periodically checking the connection status,
 * version information, etc. of all data sources and updating their status
 * </p>
 */
@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "REC_CATCH_EXCEPTION"})
public class CheckScheduledRunnable
        extends ScheduledRunnable
{
    private final SourceRepository sourceRepository;
    private final PluginManager pluginManager;

    /**
     * 构造函数
     * Constructor
     *
     * @param name 任务名称 Task name
     * @param pluginManager 插件管理器 Plugin manager
     * @param sourceRepository 数据源仓库 Source repository
     */
    public CheckScheduledRunnable(String name, PluginManager pluginManager, SourceRepository sourceRepository)
    {
        super(name);
        this.pluginManager = pluginManager;
        this.sourceRepository = sourceRepository;
    }

    /**
     * 执行定时任务
     * Execute the scheduled task
     *
     * <p>
     * 遍历所有数据源并执行健康检查
     * Iterate through all data sources and perform health checks
     * </p>
     */
    @Override
    public void run()
    {
        log.info("==================== {} started =================", this.getName());
        sourceRepository.findAll()
                .forEach(this::checkAndUpdateSource);
    }

    /**
     * 检查并更新单个数据源状态
     * Check and update status for a single data source
     *
     * @param source 待检查的数据源 The data source to be checked
     */
    private void checkAndUpdateSource(SourceEntity source)
    {
        log.info("Before check source {}", source.getName());
        pluginManager.getPlugin(source.getType())
                .ifPresentOrElse(
                        plugin -> processSourceWithPlugin(source, plugin),
                        () -> logUnavailableSource(source)
                );
    }

    /**
     * 使用插件处理数据源
     * Process data source with the corresponding plugin
     *
     * @param source 数据源 The data source
     * @param plugin 对应的插件 The corresponding plugin
     */
    private void processSourceWithPlugin(SourceEntity source, Plugin plugin)
    {
        try {
            PluginService pluginService = plugin.getService(PluginService.class);
            Response response = executePluginService(source, plugin, pluginService);
            updateSourceStatus(source, response);
            sourceRepository.save(source);
        }
        catch (Exception e) {
            handleSourceError(source, e);
        }
    }

    /**
     * 执行插件服务
     * Execute plugin service
     *
     * @param source 数据源 The data source
     * @param plugin 插件 The plugin
     * @param pluginService 插件服务 The plugin service
     * @return Response 执行响应 Execution response
     */
    private Response executePluginService(SourceEntity source, Plugin plugin, PluginService pluginService)
    {
        return pluginService.execute(
                source.toConfigure(pluginManager, plugin),
                pluginService.validator()
        );
    }

    /**
     * 更新数据源状态
     * Update data source status
     *
     * @param source 数据源 The data source
     * @param response 检查响应 Check response
     */
    private void updateSourceStatus(SourceEntity source, Response response)
    {
        source.setAvailable(response.getIsSuccessful());

        if (response.getIsSuccessful()) {
            updateSourceVersion(source, response);
        }
        else {
            markSourceUnavailable(source, response.getMessage());
        }
    }

    /**
     * 更新数据源版本信息
     * Update data source version information
     *
     * <p>
     * 处理不同格式的版本信息，包括空值、数组和普通字符串
     * Handle version information in different formats, including null values, arrays, and plain strings
     * </p>
     *
     * @param source 数据源 The data source
     * @param response 检查响应 Check response
     */
    private void updateSourceVersion(SourceEntity source, Response response)
    {
        if (response.getColumns().isEmpty()) {
            source.setVersion("-");
            return;
        }

        Optional.ofNullable(response.getColumns().get(0))
                .map(version -> {
                    if (version instanceof ArrayList) {
                        return ((ArrayList<?>) version).get(0).toString();
                    }
                    else if (version instanceof JsonNode) {
                        JsonNode jsonNode = (JsonNode) version;
                        String columnName = response.getHeaders().get(0);

                        // 处理 POJONode 类型
                        // Handle POJONode type
                        if (jsonNode.isPojo() || jsonNode.isObject()) {
                            try {
                                // 将 POJONode 转换为字符串然后重新解析
                                // Convert POJONode to string and reparse
                                String pojoString = jsonNode.toString();
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                JsonNode parsedNode = mapper.readTree(pojoString);

                                if (parsedNode.isObject()) {
                                    JsonNode valueNode = parsedNode.get(columnName);
                                    if (valueNode != null) {
                                        // 如果是对象包含 values 数组，取第一个值
                                        // If object contains values array, take first value
                                        if (valueNode.isObject() && valueNode.has("values") && valueNode.get("values").isArray()) {
                                            JsonNode valuesArray = valueNode.get("values");
                                            if (!valuesArray.isEmpty()) {
                                                return valuesArray.get(0).asText();
                                            }
                                        }
                                        return valueNode.asText();
                                    }
                                }
                                return parsedNode.asText();
                            }
                            catch (Exception e) {
                                return jsonNode.asText();
                            }
                        }

                        // 处理普通 JsonNode
                        // Handle regular JsonNode
                        JsonNode valueNode = jsonNode.get(columnName);
                        return valueNode != null ? valueNode.asText() : jsonNode.asText();
                    }
                    else if (version instanceof Map) {
                        String columnName = response.getHeaders().get(0);
                        return ((Map<?, ?>) version).get(columnName).toString();
                    }
                    return version.toString();
                })
                .ifPresent(source::setVersion);
    }

    /**
     * 标记数据源为不可用状态
     * Mark data source as unavailable
     *
     * @param source 数据源 The data source
     * @param message 错误消息 Error message
     */
    private void markSourceUnavailable(SourceEntity source, String message)
    {
        source.setAvailable(false);
        source.setVersion(null);
        source.setMessage(message);
    }

    /**
     * 处理数据源错误
     * Handle data source error
     *
     * <p>
     * 记录错误日志并更新数据源状态
     * Log error message and update data source status
     * </p>
     *
     * @param source 数据源 The data source
     * @param e 异常信息 Exception information
     */
    private void handleSourceError(SourceEntity source, Exception e)
    {
        log.error("Error processing source {}: {}", source.getName(), e.getMessage(), e);
        markSourceUnavailable(source, "Error processing source: " + e.getMessage());
        sourceRepository.save(source);
    }

    /**
     * 记录不可用数据源日志
     * Log unavailable data source
     *
     * <p>
     * 当找不到对应的插件时记录警告日志
     * Log warning message when corresponding plugin is not found
     * </p>
     *
     * @param source 数据源 The data source
     */
    private void logUnavailableSource(SourceEntity source)
    {
        log.warn("Check scheduled task <{}> source {} protocol {} is not available",
                getName(),
                source.getType(),
                source.getProtocol());
    }
}
