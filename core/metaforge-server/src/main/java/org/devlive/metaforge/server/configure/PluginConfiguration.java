package org.devlive.metaforge.server.configure;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.utils.EnvironmentUtils;
import org.devlive.metaforge.plugin.PluginConfigure;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.utils.PluginPathUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.util.Set;

@Slf4j
@Configuration
public class PluginConfiguration
{
    private final Environment environment;

    @Value(value = "${plugin.manager.extend.packages}")
    private Set<String> extendPackages;

    public PluginConfiguration(Environment environment)
    {
        this.environment = environment;
    }

    @Bean
    public PluginManager pluginManager()
    {
        EnvironmentUtils.printEnvironmentInfo();

        String root = environment.getProperty("spring.config.location");
        Path projectRoot = PluginPathUtils.findProjectRoot();
        PluginConfigure config = PluginConfigure.builder()
                .pluginsDir(PluginPathUtils.appendPath("plugins"))
                .autoCleanup(true)
                .build();

        if (extendPackages != null) {
            log.info("Extend packages: {}", extendPackages);
            config.addParentClassLoaderPackage(extendPackages);
        }

        // 开发模式下生效，如果涉及到插件的安装卸载，请注释掉这部分代码
        // In development mode, it is effective, if there is a plugin installation and uninstallation, please comment out this code
        if (EnvironmentUtils.isIdeEnvironment()) {
            log.info("Development mode is development mode");
            config.setPluginsDir(projectRoot.resolve(Path.of(String.join("/", root, "plugins.properties"))));
        }

        log.info("Plugins directory: {}", config.getPluginsDir());
        PluginManager pluginManager = new PluginManager(config);
        pluginManager.start();

        return pluginManager;
    }
}
