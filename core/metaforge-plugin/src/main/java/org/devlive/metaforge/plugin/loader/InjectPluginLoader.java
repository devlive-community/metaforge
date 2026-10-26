package org.devlive.metaforge.plugin.loader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginContextManager;
import org.devlive.metaforge.plugin.SpiType;
import org.devlive.metaforge.plugin.scanner.PluginAnnotationScanner;
import org.devlive.metaforge.plugin.utils.PluginClassLoaderUtils;
import org.devlive.metaforge.plugin.utils.VersionUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Slf4j
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
public class InjectPluginLoader
        implements PluginLoader
{
    private final PluginAnnotationScanner scanner;

    public InjectPluginLoader()
    {
        this.scanner = new PluginAnnotationScanner();
    }

    @Override
    public SpiType getType()
    {
        return SpiType.INJECT;
    }

    private Path getEffectivePluginPath(Path originalPath)
    {
        if (originalPath.endsWith("classes")) {
            return originalPath.getParent();
        }
        return originalPath;
    }

    private String getEffectivePluginName(Path path)
    {
        if (path.getFileName().toString().equals("target")) {
            return path.getParent().getFileName().toString();
        }
        if (path.getFileName().toString().equals("classes")) {
            return path.getParent().getParent().getFileName().toString();
        }
        return path.getFileName().toString();
    }

    @Override
    public List<Plugin> load(Path path, Set<String> parentClassLoaderPackages)
    {
        try {
            if (isExcludedPath(path)) {
                log.debug("Skipping excluded directory: {}", path);
                return List.of();
            }

            // 获取实际的插件路径（用于版本检测）
            // Get actual plugin path (used for version detection)
            Path effectivePath = getEffectivePluginPath(path);
            // 获取正确的插件名称
            // Get correct plugin name
            String pluginName = getEffectivePluginName(effectivePath);
            String version = VersionUtils.determinePluginVersion(effectivePath);

            if (log.isDebugEnabled()) {
                log.debug("Loading plugin - Path: {}, Effective Path: {}, Name: {}, Version: {}", path, effectivePath, pluginName, version);
            }

            // 创建插件专用类加载器
            // Create plugin-specific class loader
            PluginClassLoader classLoader = PluginClassLoaderUtils.createClassLoader(
                    path,
                    pluginName,
                    version,
                    true,
                    parentClassLoaderPackages
            );

            return PluginContextManager.runWithClassLoader(classLoader, () -> {
                List<Plugin> plugins = scanner.scanPlugins(path, classLoader);

                // 设置插件的类加载器
                // Set class loader for plugins
                plugins.forEach(plugin -> plugin.setPluginClassLoader(classLoader));

                return plugins;
            });
        }
        catch (Exception e) {
            log.error("Failed to load inject plugins from path: {}", path, e);
            return List.of();
        }
    }
}
