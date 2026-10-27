package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType

class MongoCommunityPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        return PluginConfigure.builder()
                .name("MongoDB Community")
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
                                .value(27017)
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
                                .field(PluginConfigureFieldName.ssl)
                                .type(PluginConfigureFieldType.Boolean)
                                .group(PluginConfigureFieldGroup.authorization)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.database)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .value("admin")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .value(listOf(mapOf("field" to "authenticationDatabase", "value" to "admin")))
                                .group(PluginConfigureFieldGroup.custom)
                                .build()))
                .descriptions(mapOf(
                        "zh" to "MongoDB Community 是最流行的开源文档型 NoSQL 数据库，适合灵活多变的数据结构。",
                        "en" to "MongoDB Community is the most popular open-source document-oriented NoSQL database for flexible data structures."))
                .features(mapOf(
                        "zh" to listOf("文档模型", "水平扩展", "丰富的查询能力", "高可用副本集"),
                        "en" to listOf("Document model", "Horizontal scaling", "Rich queries", "Replica sets")))
                .scenes(mapOf(
                        "zh" to listOf("内容管理", "物联网", "用户画像", "实时数据"),
                        "en" to listOf("Content management", "IoT", "User profiles", "Real-time data")))
                .category(PluginConfigureCategory.NOSQL)
                .build()
    }
}
