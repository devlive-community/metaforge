package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType
import java.util.Arrays

class ScyllaDBPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val configure = PluginConfigure.of("ScyllaDB",
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
                                .value(9042)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.database)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .value("datacenter")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build()),
                null)

        configure.descriptions = mapOf(
                "zh" to "ScyllaDB 是 Cassandra 兼容的高性能 NoSQL 数据库，以 C++ 重写实现低延迟高吞吐。",
                "en" to "ScyllaDB is a Cassandra-compatible NoSQL database rewritten in C++ for low latency and high throughput.")
        configure.features = mapOf(
                "zh" to listOf("Cassandra 兼容", "C++ 极致性能", "低延迟高吞吐", "自动分片与调优"),
                "en" to listOf("Cassandra compatible", "C++ performance", "Low latency and high throughput", "Automatic sharding and tuning"))
        configure.scenes = mapOf(
                "zh" to listOf("实时大数据", "高频写入场景", "时序与监控数据"),
                "en" to listOf("Real-time big data", "High-frequency writes", "Time-series and monitoring"))

        configure.category = PluginConfigureCategory.NOSQL

        return configure
    }
}
