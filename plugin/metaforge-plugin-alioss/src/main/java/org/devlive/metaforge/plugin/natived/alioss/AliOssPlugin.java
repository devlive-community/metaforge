package org.devlive.metaforge.plugin.natived.alioss;

import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.configure.PluginConfigure;
import org.devlive.metaforge.plugin.configure.PluginConfigureCategory;
import org.devlive.metaforge.plugin.configure.PluginConfigureField;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldGroup;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldName;
import org.devlive.metaforge.plugin.configure.PluginConfigureFieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AliOssPlugin
        extends Plugin
{
    @Override
    public PluginConfigure describe()
    {
        PluginConfigure configure = PluginConfigure.of("Alioss",
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
                                .value("https://oss-cn-regison.aliyuncs.com")
                                .message("host is a required field, please be sure to enter")
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.username)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .group(PluginConfigureFieldGroup.authorization)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.password)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .group(PluginConfigureFieldGroup.authorization)
                                .build(),
                        PluginConfigureField.builder()
                                .field(PluginConfigureFieldName.database)
                                .type(PluginConfigureFieldType.String)
                                .required(true)
                                .value("default")
                                .message("database is a required field, please be sure to enter")
                                .group(PluginConfigureFieldGroup.advanced)
                                .build()),
                null);

        configure.setDescriptions(Map.of(
                "zh", "阿里云 OSS 是海量、安全、低成本的云对象存储服务，提供 99.9999999999% 的数据可靠性。",
                "en", "Alibaba Cloud OSS is a mass-scale, secure and cost-effective cloud object storage with 12 nines of durability."));
        configure.setFeatures(Map.of(
                "zh", List.of("高持久性", "无限容量", "丰富的生态", "生命周期管理"),
                "en", List.of("High durability", "Unlimited capacity", "Rich ecosystem", "Lifecycle management")));
        configure.setScenes(Map.of(
                "zh", List.of("数据湖", "备份归档", "静态资源"),
                "en", List.of("Data lake", "Backup and archive", "Static assets")));

        configure.setCategory(PluginConfigureCategory.CLOUD);

        return configure;
    }
}
