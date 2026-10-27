package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType

import java.util.Collections

class HdfsPlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val configure = PluginConfigure.of("HDFS",
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
                                .value("-")
                                .disabled(true)
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.port)
                                .type(PluginConfigureFieldType.Number)
                                .required(true)
                                .min(1L)
                                .max(65535L)
                                .value(1)
                                .disabled(true)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.file)
                                .type(PluginConfigureFieldType.File)
                                .required(true)
                                .value(Collections.emptyList<Any>())
                                .group(PluginConfigureFieldGroup.advanced)
                                .build()),
                null)

        configure.descriptions = mapOf(
                "zh" to "HDFS 是 Hadoop 分布式文件系统，适合存储海量大数据文件。",
                "en" to "HDFS is the Hadoop distributed file system for storing massive volumes of data.")
        configure.features = mapOf(
                "zh" to listOf("高容错副本机制", "水平扩展到 PB 级", "与 Hadoop 生态集成", "适合大文件顺序读写"),
                "en" to listOf("Fault-tolerant replication", "Scales to petabytes", "Hadoop ecosystem integration", "Optimized for large files"))
        configure.scenes = mapOf(
                "zh" to listOf("数据湖存储", "离线数仓", "日志归档"),
                "en" to listOf("Data lake storage", "Offline data warehouse", "Log archiving"))

        configure.category = PluginConfigureCategory.FILE

        return configure
    }
}
