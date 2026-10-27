package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType

import java.util.Arrays

class DorisPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val configure = PluginConfigure.of("Doris",
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
                                .value(9093)
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
                "zh" to "Doris 是易用的实时分析数据库，支持高并发点查与复杂分析。",
                "en" to "Doris is an easy-to-use real-time analytical database supporting both point queries and complex analytics.")
        configure.features = mapOf(
                "zh" to listOf("简单易用", "高并发", "实时更新", "MySQL 协议"),
                "en" to listOf("Easy to use", "High concurrency", "Real-time updates", "MySQL protocol"))
        configure.scenes = mapOf(
                "zh" to listOf("报表分析", "用户行为", "数据湖"),
                "en" to listOf("Reporting", "User behavior", "Data lake"))

        configure.category = PluginConfigureCategory.BIGDATA
        return configure
    }
}
