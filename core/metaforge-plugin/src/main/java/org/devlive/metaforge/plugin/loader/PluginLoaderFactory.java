package org.devlive.metaforge.plugin.loader;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.Plugin;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PluginLoaderFactory
{
    private PluginLoaderFactory() {}

    // 用于缓存已注册的加载器
    // Cache for registered loaders
    private static final Map<String, PluginLoader> loaderRegistry = new ConcurrentHashMap<>();

    // 注册默认的加载器
    // Register default loaders
    static {
        registerLoader(new PropertiesPluginLoader());
        registerLoader(new CompiledPomPluginLoader());
        registerLoader(new DirectoryPluginLoader());
        registerLoader(new PomPluginLoader());
        registerLoader(new SpiPluginLoader());
        registerLoader(new TarPluginLoader());
    }

    /**
     * 注册一个新的插件加载器
     * Register a new plugin loader
     *
     * @param loader 要注册的加载器
     * the loader to register
     */
    public static void registerLoader(PluginLoader loader)
    {
        String type = loader.getType().getName();
        if (type == null || type.trim().isEmpty()) {
            log.warn("Attempted to register loader with null or empty type: {}", loader.getClass().getName());
            return;
        }

        PluginLoader existing = loaderRegistry.putIfAbsent(type, loader);
        if (existing != null) {
            log.warn("Loader type '{}' is already registered, skipping registration of {}", type, loader.getClass().getName());
        }
        else {
            log.info("Registered plugin loader [ {} ] for type [ {} ]", loader.getClass().getName(), type);
        }
    }

    /**
     * 移除插件加载器
     * Remove a plugin loader by type
     *
     * @param type 要移除的加载器类型
     * the type of the loader to remove
     */
    public static void unregisterLoader(String type)
    {
        loaderRegistry.remove(type);
    }

    /**
     * 根据类型获取加载器
     * Get loader by type
     *
     * @param type 加载器类型
     * loader type
     * @return 对应的加载器实例，如果未找到则返回 null
     * corresponding loader instance, or null if not found
     */
    public static PluginLoader getLoader(String type)
    {
        if (type == null) {
            return null;
        }
        return loaderRegistry.get(type.toLowerCase());
    }

    /**
     * 使用所有已注册的加载器尝试加载插件
     * Attempt to load plugins using all registered loaders
     *
     * @param pluginDir 插件目录
     * plugin directory
     * @param parentClassLoaderPackages 父类加载器包名集合
     * parent class loader package names
     * @return 加载的插件模块列表
     * list of loaded plugin modules
     */
    public static List<Plugin> loadPlugins(Path pluginDir, Set<String> parentClassLoaderPackages)
    {
        if (pluginDir == null) {
            log.warn("Plugin directory is null");
            return Collections.emptyList();
        }

        // 遍历所有注册的加载器尝试加载
        // Iterate through all registered loaders to attempt loading
        for (Map.Entry<String, PluginLoader> entry : loaderRegistry.entrySet()) {
            String type = entry.getKey();
            PluginLoader loader = entry.getValue();

            try {
                List<Plugin> modules = loader.load(pluginDir, parentClassLoaderPackages);
                if (modules != null && !modules.isEmpty()) {
                    log.info("Successfully loaded [ {} ] plugin(s) using loader type [ {} ]", modules.size(), type);
                    // 开发模式的 pom/编译目录加载器不设置 key，商店只能靠类名匹配目录清单而出现重复行；
                    // 统一以模块目录名（与 metadata.json 的 key 一致）兜底
                    // Development loaders leave the key unset, so the store matched by class name and listed
                    // plugins twice. Fall back to the module directory name, which equals the catalogue key.
                    Path module = pluginDir.toString().endsWith("pom.xml") && pluginDir.getParent() != null ? pluginDir.getParent() : pluginDir;
                    modules.forEach(v -> {
                        v.setClassLoader(loader.getClass().getName());
                        if (v.getKey() == null || v.getKey().isBlank()) {
                            v.setKey(loader.extractPluginName(module));
                        }
                    });
                    return modules;
                }
            }
            catch (Exception e) {
                log.debug("Failed to load plugins using loader type '{}': {}", type, e.getMessage());
            }
        }

        log.warn("No plugins could be loaded from directory: {}", pluginDir);
        return Collections.emptyList();
    }

    /**
     * 获取所有已注册的加载器类型
     * Get all registered loader types
     *
     * @return 加载器类型列表
     * list of loader types
     */
    public static List<String> getRegisteredTypes()
    {
        return List.copyOf(loaderRegistry.keySet());
    }

    /**
     * 清空所有已注册的加载器
     * Clear all registered loaders
     */
    public static void clear()
    {
        loaderRegistry.clear();
    }
}
