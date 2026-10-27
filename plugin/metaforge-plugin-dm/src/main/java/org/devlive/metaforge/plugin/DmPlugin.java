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

public class DmPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        return PluginConfigure.builder()
                .name("DM")
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
                                .value(5236)
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
                        "zh", "DM（达梦）是国产企业级关系型数据库，广泛服务于党政与关键行业的信创场景。",
                        "en", "DM (Dameng) is an enterprise relational database from China, widely adopted in government and mission-critical localization scenarios."))
                .features(Map.of(
                        "zh", List.of("高度兼容 Oracle", "自主可控", "高可用集群", "高级别安全防护"),
                        "en", List.of("Oracle compatible", "Independent and controllable", "High availability cluster", "Advanced security")))
                .scenes(Map.of(
                        "zh", List.of("信创替代", "党政机关", "关键行业业务系统", "数据仓库"),
                        "en", List.of("Localization migration", "Government agencies", "Mission-critical systems", "Data warehouse")))
                .category(PluginConfigureCategory.RDB)
                .build();
    }
}
