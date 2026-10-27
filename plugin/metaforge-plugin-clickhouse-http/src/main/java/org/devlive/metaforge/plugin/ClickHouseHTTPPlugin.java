package org.devlive.metaforge.plugin;

import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClickHouseHTTPPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("ClickHouse Http",
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
                                .value(8123)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.username)
                                .type(PluginConfigureFieldType.String)
                                .value("default")
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
                                .value("default")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .group(PluginConfigureFieldGroup.custom)
                                .build()),
                null);

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
