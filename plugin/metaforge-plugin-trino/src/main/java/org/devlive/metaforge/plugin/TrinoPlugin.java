package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TrinoPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("Trino",
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
                                .value(8080)
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
                                .field(PluginConfigureFieldName.catalog)
                                .type(PluginConfigureFieldType.String)
                                .value("")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .value(Collections.emptyList())
                                .group(PluginConfigureFieldGroup.custom)
                                .build()),
                null);

        configure.setDescriptions(Map.of(
                "zh", "Trino 是由 Presto 演进的高性能分布式 SQL 引擎，擅长大规模联邦查询。",
                "en", "Trino is a high-performance distributed SQL engine evolved from Presto for large-scale federated queries."));
        configure.setFeatures(Map.of(
                "zh", List.of("高性能分布式查询", "丰富的数据源连接器", "标准 SQL 兼容", "活跃社区生态"),
                "en", List.of("High-performance distributed queries", "Rich connector ecosystem", "Standard SQL", "Active community")));
        configure.setScenes(Map.of(
                "zh", List.of("交互式分析", "数据湖联邦查询", "大数据即席查询"),
                "en", List.of("Interactive analytics", "Data lake federation", "Big data ad-hoc queries")));

        configure.setCategory(PluginConfigureCategory.BIGDATA);
        return configure;
    }
}
