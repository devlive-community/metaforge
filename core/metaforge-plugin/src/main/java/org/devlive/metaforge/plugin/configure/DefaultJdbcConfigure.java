package org.devlive.metaforge.plugin.configure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 复刻原 configure/etc/conf/plugins/default.yaml 的通用 JDBC 连接字段，
 * 作为未显式声明 describe() 的连接器插件的默认表单配置。
 *
 * Generic JDBC connection fields replicated from the former default.yaml,
 * used as the fallback form configuration for connector plugins that do not
 * declare their own {@code describe()}.
 */
public final class DefaultJdbcConfigure
{
    public static final PluginConfigure INSTANCE = build();

    private DefaultJdbcConfigure() {}

    private static PluginConfigure build()
    {
        return PluginConfigure.of("Default", standardFields(), null);
    }

    /**
     * 每次调用都返回新的字段实例，避免多个插件共享可变列表。
     *
     * Returns fresh field instances on every call so plugins never share
     * a mutable configure list.
     */
    public static List<PluginConfigureField> standardFields()
    {
        return Arrays.asList(
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
                        .value(1L)
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
                        .build());
    }

    /**
     * 以通用 JDBC 字段创建指定名称的插件配置。
     *
     * Creates a plugin configure with the generic JDBC fields under the given name.
     */
    public static PluginConfigure of(String name)
    {
        return PluginConfigure.of(name, standardFields(), null);
    }
}
