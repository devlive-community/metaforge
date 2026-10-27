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

public class KyuubiPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        return PluginConfigure.builder()
                .name("Kyuubi")
                .configures(Arrays.asList(
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
                                .value(10009)
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
                                .value(Collections.emptyList())
                                .group(PluginConfigureFieldGroup.custom)
                                .build()))
                .descriptions(Map.of(
                        "zh", "Kyuubi 是基于 Spark 的分布式 SQL 网关，提供高并发的 Thrift/JDBC 服务。",
                        "en", "Kyuubi is a distributed SQL gateway built on Spark, providing highly concurrent Thrift/JDBC services."))
                .features(Map.of(
                        "zh", List.of("高并发 JDBC 服务", "多租户与资源隔离", "引擎共享与复用", "兼容 Hive Thrift 协议"),
                        "en", List.of("High-concurrency JDBC service", "Multi-tenancy and isolation", "Engine sharing and reuse", "Hive Thrift protocol compatible")))
                .scenes(Map.of(
                        "zh", List.of("SQL 即席查询", "数据仓库分析", "多租户数据平台"),
                        "en", List.of("Ad-hoc SQL queries", "Data warehouse analytics", "Multi-tenant data platforms")))
                .category(PluginConfigureCategory.BIGDATA)
                .build();
    }
}
