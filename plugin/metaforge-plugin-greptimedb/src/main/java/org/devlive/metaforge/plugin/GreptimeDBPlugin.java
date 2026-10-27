package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GreptimeDBPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("GreptimeDB",
                Arrays.asList(
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.name)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .message("name is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.host)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .value("127.0.0.1")
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.port)
                                .type(PluginConfigureFieldType.Number)
                                .required(true)
                                .min(1L)
                                .max(65535L)
                                .value(4000)
                                .message("port is a required field, please be sure to enter")
                                .build()),
                null);

        configure.setDescriptions(Map.of(
                "zh", "GreptimeDB 是云原生时序数据库，统一处理指标、日志与事件数据。",
                "en", "GreptimeDB is a cloud-native time-series database unifying metrics, logs and events."));
        configure.setFeatures(Map.of(
                "zh", List.of("云原生与存算分离", "时序与日志统一存储", "兼容 SQL 与 PromQL", "低成本高压缩"),
                "en", List.of("Cloud-native, storage-compute separation", "Unified time-series and logs", "SQL and PromQL compatible", "Cost-efficient compression")));
        configure.setScenes(Map.of(
                "zh", List.of("可观测性平台", "物联网", "基础设施监控"),
                "en", List.of("Observability platforms", "IoT", "Infrastructure monitoring")));

        configure.setCategory(PluginConfigureCategory.NOSQL);

        return configure;
    }
}
