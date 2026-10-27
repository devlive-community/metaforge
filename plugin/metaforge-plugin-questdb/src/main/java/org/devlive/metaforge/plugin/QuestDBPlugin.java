package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class QuestDBPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("QuestDB",
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
                                .value(9000)
                                .message("port is a required field, please be sure to enter")
                                .build()),
                null);

        configure.setDescriptions(Map.of(
                "zh", "QuestDB 是高性能时序数据库，以 SQL 优先设计并支持 InfluxDB 协议。",
                "en", "QuestDB is a high-performance time-series database with SQL-first design and InfluxDB protocol support."));
        configure.setFeatures(Map.of(
                "zh", List.of("超高速时序写入", "标准 SQL 查询", "InfluxDB 行协议兼容", "低资源占用"),
                "en", List.of("Ultra-fast ingestion", "Standard SQL queries", "InfluxDB line protocol", "Low resource usage")));
        configure.setScenes(Map.of(
                "zh", List.of("金融市场数据", "监控指标", "物联网遥测"),
                "en", List.of("Financial market data", "Monitoring metrics", "IoT telemetry")));

        configure.setCategory(PluginConfigureCategory.NOSQL);

        return configure;
    }
}
