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

public class DuckDBPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("DuckDB",
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
                                .value("/root")
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.port)
                                .type(PluginConfigureFieldType.Number)
                                .required(true)
                                .min(1L)
                                .max(65535L)
                                .value(0)
                                .message("port is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.database)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .value("local")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build()),
                null);

        configure.setDescriptions(Map.of(
                "zh", "DuckDB 是轻量级的嵌入式分析型数据库，适合本地快速数据分析。",
                "en", "DuckDB is a lightweight embedded analytical database for fast local data analysis."));
        configure.setFeatures(Map.of(
                "zh", List.of("嵌入式零部署", "列式 OLAP 高性能", "SQL 标准兼容", "直接查询 Parquet/CSV"),
                "en", List.of("Embedded, zero deployment", "Columnar OLAP performance", "Standard SQL", "Query Parquet/CSV directly")));
        configure.setScenes(Map.of(
                "zh", List.of("本地数据分析", "原型验证", "数据文件处理"),
                "en", List.of("Local data analysis", "Prototyping", "Data file processing")));

        configure.setCategory(PluginConfigureCategory.RDB);
        return configure;
    }
}
