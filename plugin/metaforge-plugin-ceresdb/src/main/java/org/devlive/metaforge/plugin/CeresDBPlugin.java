package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CeresDBPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("CeresDB",
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
                                .value(5440)
                                .message("port is a required field, please be sure to enter")
                                .build()),
                null);

        configure.setDescriptions(Map.of(
                "zh", "CeresDB 是云原生时序数据库，面向可观测性与物联网场景设计。",
                "en", "CeresDB is a cloud-native time-series database built for observability and IoT scenarios."));
        configure.setFeatures(Map.of(
                "zh", List.of("云原生架构", "高压缩比存储", "高写入吞吐", "兼容 SQL 与 PromQL"),
                "en", List.of("Cloud-native architecture", "High compression storage", "High write throughput", "SQL and PromQL compatible")));
        configure.setScenes(Map.of(
                "zh", List.of("监控与可观测性", "物联网数据", "指标分析"),
                "en", List.of("Monitoring and observability", "IoT data", "Metrics analytics")));

        configure.setCategory(PluginConfigureCategory.NOSQL);

        return configure;
    }
}
