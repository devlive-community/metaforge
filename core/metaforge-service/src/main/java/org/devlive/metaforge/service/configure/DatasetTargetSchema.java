package org.devlive.metaforge.service.configure;

import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginFieldType;

import java.util.Arrays;
import java.util.List;

/**
 * 数据集目标存储（落库 ClickHouse 等）的可配置字段 schema。
 * 字段与原 application.properties 中 metaforge.dataset.* 一一对应。
 *
 * Field schema for the dataset target storage (formerly bound via {@code @ConfigurationProperties(prefix = "metaforge.dataset")}).
 * Used by RuntimeConfigureService to seed / merge / serve effective values.
 */
public final class DatasetTargetSchema
{
    private DatasetTargetSchema() {}

    public static final String NAME = "Default";

    public static List<PluginConfigureField> fields()
    {
        return Arrays.asList(
                new PluginConfigureField("type", PluginFieldType.STRING, "ClickHouse",
                        "Target storage plugin type", false),
                new PluginConfigureField("host", PluginFieldType.STRING, "app-clickhouse",
                        "Target host", false),
                new PluginConfigureField("port", PluginFieldType.STRING, "8123",
                        "Target port", false),
                new PluginConfigureField("username", PluginFieldType.STRING, "default",
                        "Target username", false),
                new PluginConfigureField("password", PluginFieldType.PASSWORD, "",
                        "Target password", false),
                new PluginConfigureField("database", PluginFieldType.STRING, "metaforge",
                        "Target database", false),
                new PluginConfigureField("tableDefaultEngine", PluginFieldType.STRING, "MergeTree",
                        "Default table engine when materializing", false),
                new PluginConfigureField("tablePrefix", PluginFieldType.STRING, "metaforge_",
                        "Table name prefix", false)
        );
    }
}
