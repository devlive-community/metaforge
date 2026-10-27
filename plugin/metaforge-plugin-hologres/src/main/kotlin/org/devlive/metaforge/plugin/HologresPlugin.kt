package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType
import java.util.Arrays

class HologresPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val configure = PluginConfigure.of("Hologres",
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
                                .value("hologres-cn-regison.aliyuncs.com")
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.port)
                                .type(PluginConfigureFieldType.Number)
                                .required(true)
                                .min(1L)
                                .max(65535L)
                                .value(80)
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
                                .required(true)
                                .value("default")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build()),
                null)

        configure.descriptions = mapOf(
                "zh" to "Hologres 是阿里云的实时交互式分析引擎，兼容 PostgreSQL 生态。",
                "en" to "Hologres is Alibaba Cloud’s real-time interactive analytics engine, compatible with the PostgreSQL ecosystem.")
        configure.features = mapOf(
                "zh" to listOf("实时写入查询", "高并发低延迟", "兼容 PostgreSQL", "与 MaxCompute 集成"),
                "en" to listOf("Real-time ingestion and query", "High concurrency, low latency", "PostgreSQL compatible", "MaxCompute integration"))
        configure.scenes = mapOf(
                "zh" to listOf("实时大盘", "用户画像", "实时数仓"),
                "en" to listOf("Real-time dashboards", "User profiles", "Real-time data warehouse"))

        configure.category = PluginConfigureCategory.BIGDATA
        return configure
    }
}
