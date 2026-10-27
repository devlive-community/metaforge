package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OceanBasePlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        Map<String, Object> useOldAliasMetadataBehavior = new LinkedHashMap<>();
        useOldAliasMetadataBehavior.put("field", "useOldAliasMetadataBehavior");
        useOldAliasMetadataBehavior.put("value", true);

        PluginConfigure configure = PluginConfigure.of("OceanBase",
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
                                .value(2881)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.username)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .group(PluginConfigureFieldGroup.authorization)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.password)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
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
                null);

        configure.setDescriptions(Map.of(
                "zh", "OceanBase 是蚂蚁集团自研的分布式关系数据库，适用于金融级高可用场景。",
                "en", "OceanBase is a distributed relational database by Ant Group, built for financial-grade high availability."));
        configure.setFeatures(Map.of(
                "zh", List.of("金融级高可用", "分布式强一致", "MySQL/Oracle 兼容", "HTAP 混合负载"),
                "en", List.of("Financial-grade availability", "Distributed strong consistency", "MySQL/Oracle compatible", "HTAP workloads")));
        configure.setScenes(Map.of(
                "zh", List.of("核心交易系统", "金融行业", "大型在线业务"),
                "en", List.of("Core transaction systems", "Finance industry", "Large online businesses")));

        configure.setCategory(PluginConfigureCategory.RDB);
        return configure;
    }
}
