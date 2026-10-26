package org.devlive.metaforge.plugin.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.loader.PluginClassLoader;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 插件类加载器工具类
 * Plugin Class Loader Utility Class
 */
@Slf4j
@SuppressFBWarnings(value = {"DMI_COLLECTION_OF_URLS", "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
public class PluginClassLoaderUtils
{
    private PluginClassLoaderUtils() {}

    /**
     * 创建一个新的插件类加载器
     * Create a new plugin class loader
     *
     * @param directory 插件目录
     * Plugin directory
     * @param pluginName 插件名称
     * Plugin name
     * @param pluginVersion 插件版本
     * Plugin version
     * @param parentFirst 是否先加载父类加载器
     * Whether to load the parent class loader first
     * @return 新创建的类加载器
     * The newly created class loader
     * @throws Exception 创建类加载器时发生异常
     * Exception occurred when creating the class loader
     */
    public static PluginClassLoader createClassLoader(Path directory, String pluginName, String pluginVersion, boolean parentFirst, Set<String> parentClassLoaders)
            throws Exception
    {
        log.debug("Creating new class loader for plugin: {} version: {} directory: {}",
                pluginName, pluginVersion, directory);

        LinkedHashSet<URL> urls = new LinkedHashSet<>();

        if (Files.isDirectory(directory)) {
            // 添加主插件JAR
            // Add the main plugin JAR
            try (Stream<Path> pathStream = Files.walk(directory)) {
                pathStream.filter(path -> path.toString().endsWith(".jar"))
                        .forEach(path -> addJarAndDependencies(path, urls));
            }

            // 检查常见的依赖目录
            // Check common dependency directories
            addDependenciesFromDir(directory.resolve("lib"), urls);
            addDependenciesFromDir(directory.resolve("dependencies"), urls);
            addDependenciesFromDir(directory.resolve("target/dependencies"), urls);
        }

        // 创建独立的插件类加载器，使用系统类加载器作为父加载器
        // Create isolated plugin class loader with system class loader as parent
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        log.debug("Created plugin class loader with {} URLs", urls.size());

        return new PluginClassLoader(
                urls.toArray(new URL[0]),
                systemClassLoader,
                pluginName,
                pluginVersion,
                parentFirst,
                parentClassLoaders
        );
    }

    /**
     * 从指定目录中添加依赖JAR
     * Add dependencies from the specified directory
     *
     * @param dir 依赖目录 Dependency directory
     * @param urls URL集合 URL set
     */
    private static void addDependenciesFromDir(Path dir, LinkedHashSet<URL> urls)
    {
        if (Files.isDirectory(dir)) {
            try {
                log.debug("Scanning dependency directory: {}", dir);
                try (Stream<Path> pathStream = Files.walk(dir)) {
                    pathStream.filter(path -> path.toString().endsWith(".jar"))
                            .forEach(path -> {
                                try {
                                    urls.add(path.toUri().toURL());
                                    log.debug("Added dependency: {}", path);
                                }
                                catch (Exception e) {
                                    log.error("Failed to add dependency: {}", path, e);
                                    throw new RuntimeException("Failed to add dependency: " + path, e);
                                }
                            });
                }
            }
            catch (Exception e) {
                log.error("Failed to scan dependency directory: {}", dir, e);
                throw new RuntimeException("Failed to scan dependency directory: " + dir, e);
            }
        }
    }

    /**
     * 添加JAR文件及其依赖
     * Add a JAR file and its dependencies
     *
     * @param jarPath JAR文件路径 JAR file path
     * @param urls URL集合 URL set
     */
    private static void addJarAndDependencies(Path jarPath, LinkedHashSet<URL> urls)
    {
        try {
            urls.add(jarPath.toUri().toURL());
            log.debug("Added JAR: {}", jarPath);
        }
        catch (Exception e) {
            log.error("Failed to add JAR: {}", jarPath, e);
            throw new RuntimeException("Failed to add JAR: " + jarPath, e);
        }
    }
}
