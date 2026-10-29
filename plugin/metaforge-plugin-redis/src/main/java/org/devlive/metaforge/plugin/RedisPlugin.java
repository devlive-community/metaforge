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

public class RedisPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("Redis",
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
                                .value(6379)
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
                                .value("0")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.configures)
                                .type(PluginConfigureFieldType.Array)
                                .group(PluginConfigureFieldGroup.custom)
                                .build()),
                null);

        configure.setDescriptions(Map.of(
                "zh", "Redis 是高性能的内存键值数据库，广泛用于缓存与消息场景。",
                "en", "Redis is a high-performance in-memory key-value store, widely used for caching and messaging."));
        configure.setFeatures(Map.of(
                "zh", List.of("亚毫秒级读写", "丰富数据结构", "持久化支持", "主从与集群"),
                "en", List.of("Sub-millisecond R/W", "Rich data structures", "Persistence", "Replication and cluster")));
        configure.setScenes(Map.of(
                "zh", List.of("缓存加速", "会话存储", "排行榜", "消息队列"),
                "en", List.of("Caching", "Session storage", "Leaderboards", "Message queue")));

        configure.setCategory(PluginConfigureCategory.NOSQL);

        return configure;
    }
}
