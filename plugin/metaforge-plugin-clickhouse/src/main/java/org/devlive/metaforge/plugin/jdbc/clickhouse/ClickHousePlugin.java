package org.devlive.metaforge.plugin.jdbc.clickhouse;

import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.configure.FieldType;
import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureExecutor;
import org.devlive.metaforge.plugin.configure.PluginConfigureExecutorField;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;
import org.devlive.metaforge.plugin.configure.PluginConfigurePipelineType;
import org.devlive.metaforge.plugin.configure.RunProtocol;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClickHousePlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("ClickHouse",
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
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.database)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .value("default")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .value(Collections.emptyList())
                                .group(PluginConfigureFieldGroup.custom)
                                .build()),
                Arrays.asList(
                        PluginConfigureExecutor.of("Seatunnel", PluginConfigurePipelineType.INPUT, RunProtocol.NONE,
                                Arrays.asList(
                                        PluginConfigureExecutorField.builder()
                                                .field("host")
                                                .origin("host|port")
                                                .required(true)
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("username")
                                                .origin("username")
                                                .required(true)
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("password")
                                                .origin("password")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("database")
                                                .origin("database")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .description("The ClickHouse database")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("sql")
                                                .origin("context")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("The query sql used to search data though Clickhouse server")
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
                        PluginConfigureExecutor.of("Seatunnel", PluginConfigurePipelineType.OUTPUT, RunProtocol.NONE,
                                Arrays.asList(
                                        PluginConfigureExecutorField.builder()
                                                .field("host")
                                                .origin("host|port")
                                                .required(true)
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("username")
                                                .origin("username")
                                                .required(true)
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("password")
                                                .required(true)
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("database")
                                                .origin("database")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .description("The ClickHouse database")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("table")
                                                .origin("table")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .description("The table name of the ClickHouse database")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("fields")
                                                .origin("fields")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("The data field that needs to be output to ClickHouse , if not configured, it will be automatically adapted according to the sink table schema .\n"
                                                        + "\n"
                                                        + "If there are multiple values, each value needs to be one row.")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("bulk_size")
                                                .origin("bulk_size")
                                                .override(true)
                                                .input(true)
                                                .width(100)
                                                .type(FieldType.NUMBER)
                                                .value(20000)
                                                .description("The number of rows written through Clickhouse-jdbc each time, the default is 20000 .")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("split_mode")
                                                .origin("split_mode")
                                                .override(true)
                                                .input(true)
                                                .width(45)
                                                .type(FieldType.SWITCH)
                                                .value(false)
                                                .description("This mode only support clickhouse table which engine is 'Distributed'.And internal_replication option should be true. They will split distributed table data in seatunnel and perform write directly on each shard. The shard weight define is clickhouse will be counted.")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("sharding_key")
                                                .origin("sharding_key")
                                                .override(true)
                                                .input(true)
                                                .width(100)
                                                .description("When use split_mode, which node to send data to is a problem, the default is random selection, but the 'sharding_key' parameter can be used to specify the field for the sharding algorithm. This option only worked when 'split_mode' is true.")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("primary_key")
                                                .origin("primary_key")
                                                .override(true)
                                                .input(true)
                                                .width(100)
                                                .description("Mark the primary key column from clickhouse table, and based on primary key execute INSERT/UPDATE/DELETE to clickhouse table")
                                                .build(),
                                        PluginConfigureExecutorField.builder()
                                                .field("support_upsert")
                                                .origin("support_upsert")
                                                .override(true)
                                                .input(true)
                                                .width(45)
                                                .type(FieldType.SWITCH)
                                                .value(false)
                                                .description("Support upsert row by query primary key")
                                                .build()))));

        configure.setDescriptions(Map.of(
                "zh", "ClickHouse 是高性能的列式 OLAP 数据库，适合海量数据的实时分析。",
                "en", "ClickHouse is a high-performance columnar OLAP database for real-time analytics on massive data."));
        configure.setFeatures(Map.of(
                "zh", List.of("列式存储", "极致查询性能", "数据压缩", "实时写入"),
                "en", List.of("Columnar storage", "Blazing queries", "Compression", "Real-time ingestion")));
        configure.setScenes(Map.of(
                "zh", List.of("用户行为分析", "监控大盘", "商业智能", "日志分析"),
                "en", List.of("Behavior analytics", "Monitoring dashboards", "BI", "Log analytics")));
        configure.setPopular(true);

        configure.setCategory(PluginConfigureCategory.BIGDATA);
        return configure;
    }
}
