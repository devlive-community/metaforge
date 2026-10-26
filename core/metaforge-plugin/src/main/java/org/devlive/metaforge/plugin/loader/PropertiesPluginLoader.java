package org.devlive.metaforge.plugin.loader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.SpiType;
import org.devlive.metaforge.plugin.utils.PluginPathUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Slf4j
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "OBL_UNSATISFIED_OBLIGATION"})
public class PropertiesPluginLoader
        implements PluginLoader
{
    @Override
    public SpiType getType()
    {
        return SpiType.PROPERTIES;
    }

    @Override
    public List<Plugin> load(Path path, Set<String> parentClassLoaderPackages)
    {
        List<Plugin> plugins = new ArrayList<>();
        try {
            Path propertiesPath;
            if (path.toString().endsWith("pom.xml")) {
                propertiesPath = path.getParent().resolve("plugin.properties");
            }
            else {
                propertiesPath = path;
            }

            if (!propertiesPath.toFile().exists() || propertiesPath.toFile().isDirectory()) {
                log.debug("Properties file not found: {}", propertiesPath);
                return plugins;
            }

            try (FileInputStream inputStream = new FileInputStream(propertiesPath.toFile())) {
                Properties props = new Properties();
                props.load(inputStream);
                Path baseDir = propertiesPath.getParent();

                for (String value : props.stringPropertyNames()) {
                    String pluginPath = props.getProperty(value);
                    if (pluginPath == null || pluginPath.trim().isEmpty()) {
                        continue;
                    }

                    Path other = Paths.get(pluginPath);
                    Path resolvedPath = baseDir.resolve(other);
                    log.info("Loading plugin from [ {} ]", resolvedPath);

                    if (!resolvedPath.toFile().exists()) {
                        log.info("Plugin file [ {} ] does not exist", resolvedPath);
                        log.info("Retrying with module root as base directory");
                        resolvedPath = PluginPathUtils.findProjectRoot()
                                .resolve(other)
                                .getParent();
                        log.info("Retrying module root as base directory [ {} ]", resolvedPath);
                    }

                    // 使用 SpiPluginLoader 加载
                    // Use SpiPluginLoader to load
                    SpiPluginLoader compiledLoader = new SpiPluginLoader();
                    List<Plugin> loadedPlugins = compiledLoader.load(resolvedPath, parentClassLoaderPackages);
                    if (!loadedPlugins.isEmpty()) {
                        plugins.addAll(loadedPlugins);
                    }
                    else {
                        loadedPlugins = PluginLoaderFactory.loadPlugins(resolvedPath, parentClassLoaderPackages);
                        plugins.addAll(loadedPlugins);
                    }
                }
            }
        }
        catch (IOException e) {
            log.error("Failed to load plugins from properties file: {}", path, e);
        }
        return plugins;
    }
}
