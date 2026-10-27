package org.devlive.metaforge.plugin;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MySQLPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        Map<String, Object> useOldAliasMetadataBehavior = new LinkedHashMap<>();
        useOldAliasMetadataBehavior.put("field", "useOldAliasMetadataBehavior");
        useOldAliasMetadataBehavior.put("value", true);

        PluginConfigure configure = PluginConfigure.of("MySQL",
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
                                .value(3306)
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
                                .value("default")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .value(Collections.singletonList(useOldAliasMetadataBehavior))
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
                                                .value("com.mysql.cj.jdbc.Driver")
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
                                                .value("com.mysql.cj.jdbc.Driver")
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

        configure.setDescriptions(Map.of(
                "zh", "MySQL 是最流行的开源关系型数据库，适用于各类业务系统的数据存储。",
                "en", "MySQL is the most popular open-source relational database for all kinds of business systems."));
        configure.setFeatures(Map.of(
                "zh", List.of("开源免费", "高性能和稳定性", "广泛的社区支持", "支持大规模数据处理"),
                "en", List.of("Open source", "High performance and stability", "Wide community support", "Large-scale data processing")));
        configure.setScenes(Map.of(
                "zh", List.of("业务系统", "数据仓库", "数据分析", "报表系统", "实时数据同步"),
                "en", List.of("Business systems", "Data warehouse", "Data analysis", "Reporting", "Real-time sync")));
        configure.setPopular(true);
        configure.setCategory(PluginConfigureCategory.RDB);

        return configure;
    }
}
