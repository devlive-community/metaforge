package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType

class TimescalePlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        return PluginConfigure.builder()
                .name("TimescaleDB")
                .configures(listOf(
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
                                .value(5432)
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
                                .field(PluginConfigureFieldName.database)
                                .type(PluginConfigureFieldType.String)
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .value(listOf<Any>())
                                .group(PluginConfigureFieldGroup.custom)
                                .build()))
                .descriptions(mapOf(
                        "zh" to "TimescaleDB 是基于 PostgreSQL 的时序数据库，专为时间序列与实时分析优化。",
                        "en" to "TimescaleDB is a PostgreSQL-based time-series database optimized for time-series and real-time analytics."))
                .features(mapOf(
                        "zh" to listOf("兼容 PostgreSQL 生态", "原生 hypertable 时序表", "自动分区与数据压缩", "连续聚合"),
                        "en" to listOf("PostgreSQL compatible", "Native hypertables", "Automatic partitioning and compression", "Continuous aggregates")))
                .scenes(mapOf(
                        "zh" to listOf("物联网监控", "指标采集", "实时大盘"),
                        "en" to listOf("IoT monitoring", "Metrics collection", "Real-time dashboards")))
                .category(PluginConfigureCategory.RDB)
                .build()
    }
}
