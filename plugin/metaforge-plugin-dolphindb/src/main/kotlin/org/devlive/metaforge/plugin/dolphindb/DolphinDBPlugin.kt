package org.devlive.metaforge.plugin.dolphindb

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

class DolphinDBPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val database: MutableMap<String, Any> = LinkedHashMap()
        database["field"] = "database"
        database["value"] = "default"

        val configure = PluginConfigure.of("DolphinDB",
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
                                .value("8848")
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
                "zh" to "DolphinDB 是高性能时序数据库与计算平台，专为金融高频数据处理设计。",
                "en" to "DolphinDB is a high-performance time-series database and computing platform designed for financial data.")
        configure.features = mapOf(
                "zh" to listOf("时序数据高速写入", "内置计算引擎与脚本", "流式计算", "金融分析函数库"),
                "en" to listOf("High-speed time-series ingestion", "Built-in computing engine", "Stream computing", "Financial analytics library"))
        configure.scenes = mapOf(
                "zh" to listOf("量化投研", "高频行情处理", "实时风控"),
                "en" to listOf("Quantitative research", "Market data processing", "Real-time risk control"))

        configure.category = PluginConfigureCategory.NOSQL

        return configure
    }
}
