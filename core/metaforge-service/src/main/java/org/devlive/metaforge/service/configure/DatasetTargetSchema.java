package org.devlive.metaforge.service.configure;

import org.devlive.metaforge.plugin.configure.RuntimeConfigureField;
import org.devlive.metaforge.plugin.configure.RuntimeConfigureFieldType;

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

    public static List<RuntimeConfigureField> fields()
    {
        return Arrays.asList(
                new RuntimeConfigureField("type", RuntimeConfigureFieldType.STRING, "ClickHouse",
                        "Target storage plugin type", false),
                new RuntimeConfigureField("host", RuntimeConfigureFieldType.STRING, "app-clickhouse",
                        "Target host", false),
                new RuntimeConfigureField("port", RuntimeConfigureFieldType.STRING, "8123",
                        "Target port", false),
                new RuntimeConfigureField("username", RuntimeConfigureFieldType.STRING, "default",
                        "Target username", false),
                new RuntimeConfigureField("password", RuntimeConfigureFieldType.PASSWORD, "",
                        "Target password", false),
                new RuntimeConfigureField("database", RuntimeConfigureFieldType.STRING, "metaforge",
                        "Target database", false),
                new RuntimeConfigureField("tableDefaultEngine", RuntimeConfigureFieldType.STRING, "MergeTree",
                        "Default table engine when materializing", false),
                new RuntimeConfigureField("tablePrefix", RuntimeConfigureFieldType.STRING, "metaforge_",
                        "Table name prefix", false)
        );
    }
}
