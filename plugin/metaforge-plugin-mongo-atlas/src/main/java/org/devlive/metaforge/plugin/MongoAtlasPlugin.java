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

public class MongoAtlasPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        Map<String, Object> authenticationDatabase = new LinkedHashMap<>();
        authenticationDatabase.put("field", "authenticationDatabase");
        authenticationDatabase.put("value", "admin");

        return PluginConfigure.builder()
                .name("MongoDB Atlas")
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
                                .value("cluster0.xxxxx.mongodb.net")
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.port)
                                .type(PluginConfigureFieldType.Number)
                                .required(true)
                                .min(1L)
                                .max(65535L)
                                .value(27017)
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
                                .field(PluginConfigureFieldName.ssl)
                                .type(PluginConfigureFieldType.Boolean)
                                .value(true)
                                .group(PluginConfigureFieldGroup.authorization)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.database)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .value("admin")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .value(Collections.singletonList(authenticationDatabase))
                                .group(PluginConfigureFieldGroup.custom)
                                .build()))
                .descriptions(Map.of(
                        "zh", "MongoDB Atlas 是 MongoDB 官方推出的全托管云数据库服务，免运维、开箱即用。",
                        "en", "MongoDB Atlas is the fully managed cloud database service from MongoDB, zero maintenance and ready to use."))
                .features(Map.of(
                        "zh", List.of("全托管云服务", "跨云多区域部署", "内置安全与合规", "弹性扩缩容"),
                        "en", List.of("Fully managed", "Multi-cloud deployment", "Built-in security and compliance", "Elastic scaling")))
                .scenes(Map.of(
                        "zh", List.of("云上业务系统", "全球分布式应用", "数据分析", "备份与灾备"),
                        "en", List.of("Cloud applications", "Global distributed apps", "Data analytics", "Backup and recovery")))
                .category(PluginConfigureCategory.NOSQL)
                .build();
    }
}
