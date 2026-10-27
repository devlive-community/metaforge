package org.devlive.metaforge.plugin.solr

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType

import java.util.Arrays
import java.util.Collections

class SolrPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val configure = PluginConfigure.of("Apache Solr",
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
                                .value("8983")
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
                                .value("local")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .value(Collections.emptyList<Any>())
                                .group(PluginConfigureFieldGroup.custom)
                                .build()),
                null)

        configure.descriptions = mapOf(
                "zh" to "Apache Solr 是成熟的企业级搜索平台，支持全文检索与聚合分析。",
                "en" to "Apache Solr is a mature enterprise search platform for full-text search and faceted analytics.")
        configure.features = mapOf(
                "zh" to listOf("全文检索", "分面与聚合", "高可扩展集群", "丰富的解析器"),
                "en" to listOf("Full-text search", "Facets and analytics", "Scalable cluster", "Rich analyzers"))
        configure.scenes = mapOf(
                "zh" to listOf("电商搜索", "企业内容检索", "日志检索"),
                "en" to listOf("E-commerce search", "Enterprise content search", "Log search"))

        configure.category = PluginConfigureCategory.NOSQL

        return configure
    }
}
