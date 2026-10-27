package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType

import java.util.Arrays

class PinotPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val configure = PluginConfigure.of("Pinot",
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
                                .build()),
                null)

        configure.descriptions = mapOf(
                "zh" to "Apache Pinot 是面向实时分析的分布式 OLAP 引擎，支持毫秒级查询。",
                "en" to "Apache Pinot is a distributed OLAP engine for real-time analytics with millisecond queries.")
        configure.features = mapOf(
                "zh" to listOf("毫秒级查询", "实时与离线摄入", "高并发多维分析", "与 Kafka/Spark 集成"),
                "en" to listOf("Millisecond queries", "Real-time and batch ingestion", "High-concurrency analytics", "Kafka and Spark integration"))
        configure.scenes = mapOf(
                "zh" to listOf("用户行为实时分析", "实时大盘", "A/B 实验平台"),
                "en" to listOf("Real-time user analytics", "Live dashboards", "A/B testing platforms"))

        configure.category = PluginConfigureCategory.BIGDATA
        return configure
    }
}
