package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.configure.FieldType
import org.devlive.metaforge.plugin.configure.PluginConfigure
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory
import org.devlive.metaforge.plugin.configure.PluginConfigureExecutor
import org.devlive.metaforge.plugin.configure.PluginConfigureExecutorField
import org.devlive.metaforge.plugin.configure.PluginConfigureField
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType
import org.devlive.metaforge.plugin.configure.PluginConfigurePipelineType
import org.devlive.metaforge.plugin.configure.RunProtocol

import java.util.Arrays
import java.util.Collections

class MatrixOnePlugin : Plugin()
{
    override fun describe(): PluginConfigure
    {
        val configure = PluginConfigure.of("MatrixOne",
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
                                .value(6001)
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
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .value(Collections.emptyList<Any>())
                                .group(PluginConfigureFieldGroup.custom)
                                .build()),
                Arrays.asList(
                        PluginConfigureExecutor.of("Seatunnel", PluginConfigurePipelineType.INPUT, RunProtocol.JDBC,
                                Arrays.asList(
                                        PluginConfigureExecutorField.builder()
                                                .field("url")
                                                .generated(true)
                                                .override(true)
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("driver")
                                                .origin("driver")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .value("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("user")
                                                .origin("username")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("password")
                                                .origin("password")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("query")
                                                .origin("context")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("Query statement")
                                                .tooltip("If this value is entered, the default sql statement will be replaced")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("result_table_name")
                                                .origin("result_table_name")
                                                .hidden(true)
                                                .override(true)
                                                .input(true)
                                                .description("When result_table_name is not specified, the data processed by this plugin will not be registered as a data set (dataStream/dataset) that can be directly accessed by other plugins, or called a temporary table (table) ;\n"
                                                        + "When result_table_name is specified, the data processed by this plugin will be registered as a data set (dataStream/dataset) that can be directly accessed by other plugins, or called a temporary table (table) . The data set (dataStream/dataset) registered here can be directly accessed by other plugins by specifying source_table_name .")
                                                .build())),
                        PluginConfigureExecutor.of("Seatunnel", PluginConfigurePipelineType.OUTPUT, RunProtocol.JDBC,
                                Arrays.asList(
                                        PluginConfigureExecutorField.builder()
                                                .field("url")
                                                .generated(true)
                                                .override(true)
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("driver")
                                                .origin("driver")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .value("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("user")
                                                .origin("username")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("password")
                                                .origin("password")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("query")
                                                .origin("query")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("Use this sql write upstream input datas to database. e.g INSERT ...")
                                                .build()))));

        configure.descriptions = mapOf(
                "zh" to "MatrixOne 是云原生超融合数据库，单引擎同时支持事务与分析负载。",
                "en" to "MatrixOne is a cloud-native hyper-converged database supporting transactional and analytical workloads in one engine.")
        configure.features = mapOf(
                "zh" to listOf("云原生架构", "HTAP 混合负载", "MySQL 兼容", "存算分离"),
                "en" to listOf("Cloud-native architecture", "HTAP workloads", "MySQL compatible", "Storage-compute separation"))
        configure.scenes = mapOf(
                "zh" to listOf("实时数仓", "HTAP 业务", "多租户 SaaS"),
                "en" to listOf("Real-time data warehouse", "HTAP workloads", "Multi-tenant SaaS"))

        configure.category = PluginConfigureCategory.RDB
        return configure
    }
}
