package org.devlive.metaforge.plugin.influxdb

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType

import java.util.Arrays
import java.util.Collections
import java.util.LinkedHashMap

class InfluxdbPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val database: MutableMap<String, Any> = LinkedHashMap()
        database.put("field", "database")
        database.put("value", "default")

        val configure = PluginConfigure.of("InfluxDB",
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
                                .value("8086")
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.username)
                                .type(PluginConfigureFieldType.String)
                                .group(PluginConfigureFieldGroup.authorization)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.password)
                                .type(PluginConfigureFieldType.String)
                                .group(PluginConfigureFieldGroup.authorization)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .value(Collections.singletonList(database))
                                .group(PluginConfigureFieldGroup.custom)
                                .build()),
                null)

        configure.descriptions = mapOf(
                "zh" to "InfluxDB 是流行的时序数据库，专为监控指标与物联网数据设计。",
                "en" to "InfluxDB is a popular time-series database designed for monitoring metrics and IoT data.")
        configure.features = mapOf(
                "zh" to listOf("高性能时序写入", "内置 Flux/InfluxQL 查询", "数据保留策略", "连续聚合"),
                "en" to listOf("Fast time-series ingestion", "Flux and InfluxQL queries", "Retention policies", "Continuous aggregates"))
        configure.scenes = mapOf(
                "zh" to listOf("监控告警", "物联网传感数据", "指标采集分析"),
                "en" to listOf("Monitoring and alerting", "IoT sensor data", "Metrics analytics"))

        configure.category = PluginConfigureCategory.NOSQL

        return configure
    }
}
