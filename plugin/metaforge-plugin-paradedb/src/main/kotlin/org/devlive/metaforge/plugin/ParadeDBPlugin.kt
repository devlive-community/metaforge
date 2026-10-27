package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType

class ParadeDBPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        return PluginConfigure.builder()
                .name("ParadeDB")
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
                        "zh" to "ParadeDB 是构建在 PostgreSQL 之上的搜索与分析数据库，提供原生全文检索能力。",
                        "en" to "ParadeDB is a search and analytics database built on PostgreSQL, offering native full-text search."))
                .features(mapOf(
                        "zh" to listOf("兼容 PostgreSQL 生态", "内置全文检索与 BM25 评分", "列式存储加速分析", "混合搜索"),
                        "en" to listOf("PostgreSQL compatible", "Full-text search with BM25 scoring", "Columnar storage for analytics", "Hybrid search")))
                .scenes(mapOf(
                        "zh" to listOf("日志检索", "电商搜索", "数据分析"),
                        "en" to listOf("Log search", "E-commerce search", "Data analytics")))
                .category(PluginConfigureCategory.RDB)
                .build()
    }
}
