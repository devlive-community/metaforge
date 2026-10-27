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

public class PhoenixPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        return PluginConfigure.builder()
                .name("Phoenix")
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
                                .value(8765)
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
                        "zh", "Apache Phoenix 是构建在 HBase 之上的 SQL 层，可使用标准 SQL 查询 HBase 中的数据。",
                        "en", "Apache Phoenix is a SQL layer on top of HBase, enabling standard SQL queries over HBase data."))
                .features(Map.of(
                        "zh", List.of("SQL on HBase", "二级索引", "低延迟在线查询", "与 HBase 生态深度集成"),
                        "en", List.of("SQL on HBase", "Secondary indexes", "Low-latency online queries", "Deep HBase ecosystem integration")))
                .scenes(Map.of(
                        "zh", List.of("HBase 在线查询", "实时明细检索", "大数据点查场景"),
                        "en", List.of("Online HBase queries", "Real-time detail lookup", "Large-scale point queries")))
                .category(PluginConfigureCategory.BIGDATA)
                .build();
    }
}
