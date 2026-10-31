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

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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

        Path projectRoot = PluginPathUtils.findProjectRoot();
        PluginConfigure config = PluginConfigure.builder()
                .pluginsDir(pluginsDirectory(projectRoot, EnvironmentUtils.isIdeEnvironment()))
                .loadThreads(environment.getProperty("plugin.manager.load-threads", Integer.class, 0))
                .autoCleanup(true)
                .build();

        if (extendPackages != null) {
            log.info("Extend packages: {}", extendPackages);
            config.addParentClassLoaderPackage(extendPackages);
        }

        log.info("Plugins directory: {}", config.getPluginsDir());
        PluginManager pluginManager = new PluginManager(config);
        pluginManager.start();

        return pluginManager;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "findProjectRoot may report null; the method falls back to the installed plugins directory")
    Path pluginsDirectory(Path projectRoot, boolean development)
    {
        if (projectRoot == null) {
            return Path.of("plugins");
        }
        if (development) {
            for (String property : new String[] {"spring.config.additional-location", "spring.config.location"}) {
                String locations = environment.getProperty(property, "");
                String[] entries = locations.split(",");
                for (int index = entries.length - 1; index >= 0; index--) {
                    String location = entries[index].trim().replaceFirst("^optional:", "").replaceFirst("^file:", "");
                    if (location.isEmpty() || location.startsWith("classpath:")) {
                        continue;
                    }
                    try {
                        Path path = projectRoot.resolve(location).normalize();
                        Path directory = Files.isRegularFile(path) ? path.getParent() : path;
                        Path properties = directory.resolve("plugins.properties");
                        if (Files.isRegularFile(properties)) {
                            return properties;
                        }
                    }
                    catch (InvalidPathException e) {
                        log.debug("Invalid development plugin configuration location: {}", location, e);
                    }
                }
            }
            for (String location : new String[] {"config/plugins.properties", "configure/etc/conf/plugins.properties"}) {
                Path properties = projectRoot.resolve(location);
                if (Files.isRegularFile(properties)) {
                    return properties;
                }
            }
            log.warn("Development plugin manifest not found; using the installed plugins directory");
        }
        return projectRoot.resolve("plugins");
    }
}
