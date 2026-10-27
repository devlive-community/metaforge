package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType

class CassandraPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val configure = PluginConfigure.of("Apache Cassandra",
                listOf(
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
                "zh" to "Apache Cassandra 是高可用的分布式 NoSQL 数据库，擅长多数据中心大规模写入。",
                "en" to "Apache Cassandra is a highly available distributed NoSQL database built for massive writes across data centers.")
        configure.features = mapOf(
                "zh" to listOf("无单点高可用", "线性水平扩展", "多数据中心复制", "CQL 查询语言"),
                "en" to listOf("No single point of failure", "Linear horizontal scaling", "Multi-datacenter replication", "CQL query language"))
        configure.scenes = mapOf(
                "zh" to listOf("物联网时序数据", "消息与日志存储", "高可用业务数据"),
                "en" to listOf("IoT time-series data", "Messaging and logs", "High-availability workloads"))

        configure.category = PluginConfigureCategory.NOSQL

        return configure
    }
}
