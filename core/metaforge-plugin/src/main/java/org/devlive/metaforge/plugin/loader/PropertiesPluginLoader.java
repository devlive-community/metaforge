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
import java.util.stream.Collectors;

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
        for (Path resolvedPath : resolvePaths(path)) {
            List<Plugin> loadedPlugins = new SpiPluginLoader().load(resolvedPath, parentClassLoaderPackages);
            if (loadedPlugins.isEmpty()) {
                loadedPlugins = PluginLoaderFactory.loadPlugins(resolvedPath, parentClassLoaderPackages);
            }
            plugins.addAll(loadedPlugins);
        }
        return plugins;
    }

    // 只解析清单，让 PluginManager 把每个条目交给同一个有界线程池
    // Resolve manifests without loading so the manager can use one bounded worker pool
    public List<Path> resolvePaths(Path path)
    {
        List<Path> paths = new ArrayList<>();
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
                return paths;
            }

            try (FileInputStream inputStream = new FileInputStream(propertiesPath.toFile())) {
                Properties props = new Properties();
                props.load(inputStream);
                Path baseDir = propertiesPath.toAbsolutePath().getParent();

                for (String value : props.stringPropertyNames().stream().sorted().collect(Collectors.toList())) {
                    String pluginPath = props.getProperty(value);
                    if (pluginPath == null || pluginPath.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        Path other = Paths.get(pluginPath.trim());
                        Path resolvedPath = baseDir.resolve(other).normalize();
                        if (!resolvedPath.toFile().exists()) {
                            resolvedPath = PluginPathUtils.findProjectRoot().resolve(other).normalize();
                            if (resolvedPath.endsWith("pom.xml")) {
                                resolvedPath = resolvedPath.getParent();
                            }
                        }
                        paths.add(resolvedPath);
                    }
                    catch (IllegalArgumentException e) {
                        log.warn("Invalid plugin path for manifest entry: {}", value, e);
                    }
                }
            }
        }
        catch (IOException | IllegalArgumentException e) {
            log.error("Failed to load plugins from properties file: {}", path, e);
        }
        return paths;
    }
}
