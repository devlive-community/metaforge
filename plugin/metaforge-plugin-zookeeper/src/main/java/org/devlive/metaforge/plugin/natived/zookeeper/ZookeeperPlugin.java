package org.devlive.metaforge.plugin.natived.zookeeper;

import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ZookeeperPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("Zookeeper",
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
                                .value("127.0.0.1:2181")
                                .message("host is a required field, please be sure to enter")
                                .build()),
                null);

        configure.setDescriptions(Map.of(
                "zh", "Zookeeper 是分布式协调服务，广泛用于配置管理、服务注册与分布式锁。",
                "en", "Zookeeper is a distributed coordination service for configuration management, service registry and distributed locks."));
        configure.setFeatures(Map.of(
                "zh", List.of("分布式协调", "高可用集群", "顺序一致性", "Watch 通知机制"),
                "en", List.of("Distributed coordination", "High-availability ensemble", "Sequential consistency", "Watch notifications")));
        configure.setScenes(Map.of(
                "zh", List.of("配置中心", "服务注册与发现", "分布式锁"),
                "en", List.of("Configuration center", "Service registry and discovery", "Distributed locks")));

        configure.setCategory(PluginConfigureCategory.OTHER);

        return configure;
    }
}
