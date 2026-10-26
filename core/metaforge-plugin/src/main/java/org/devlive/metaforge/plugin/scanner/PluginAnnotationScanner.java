package org.devlive.metaforge.plugin.scanner;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.annotation.InjectPlugin;
import org.devlive.metaforge.plugin.loader.PluginClassLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class PluginAnnotationScanner
{
    // 定义要排除的目录
    // Define directories to exclude
    private static final Set<String> EXCLUDED_DIRS = new HashSet<>()
    {{
        add("target/test-classes");
        add("target/generated-sources");
        add("target/generated-test-sources");
        add("target/maven-status");
        add("target/maven-archiver");
        add("target/surefire-reports");
        add(".git");
        add(".idea");
        add("node_modules");
    }};

    /**
     * 扫描目录中的插件注解
     * Scan plugins in directory
     */
    public List<Plugin> scanPlugins(Path directory, PluginClassLoader classLoader)
    {
        List<Plugin> plugins = new ArrayList<>();

        try {
            // 获取要扫描的目录
            // Get directories to scan
            List<Path> scanDirs = new ArrayList<>();

            // 检查主类目录
            // Check main classes directory
            Path mainClassesDir = directory;
            if (Files.exists(directory.resolve("target/classes"))) {
                mainClassesDir = directory.resolve("target/classes");
            }
            scanDirs.add(mainClassesDir);

            // 扫描每个目录
            // Scan each directory
            for (Path scanDir : scanDirs) {
                try (Stream<Path> paths = Files.walk(scanDir)) {
                    paths.filter(path -> !isExcludedPath(path))
                            .filter(path -> path.toString().endsWith(".class"))
                            .filter(path -> !path.toString().contains("$"))
                            .forEach(path -> {
                                try {
                                    String className = getClassName(scanDir, path);
                                    Class<?> cls = classLoader.loadClass(className);

                                    // 检查是否有 @InjectPlugin 注解
                                    // Check if it has @InjectPlugin annotation
                                    InjectPlugin annotation = cls.getAnnotation(InjectPlugin.class);
                                    if (annotation != null) {
                                        processPluginClass(cls, annotation, plugins, classLoader);
                                    }
                                }
                                catch (Exception e) {
                                    log.debug("Failed to load class: {}", path);
                                }
                            });
                }
            }
        }
        catch (Exception e) {
            log.error("Failed to scan plugins in directory: {}", directory, e);
        }

        return plugins;
    }

    /**
     * 检查路径是否应该被排除
     * Check if path should be excluded
     */
    private boolean isExcludedPath(Path path)
    {
        String pathStr = path.toString();
        return EXCLUDED_DIRS.stream()
                .anyMatch(dir -> pathStr.contains(dir.replace('/', File.separatorChar)));
    }

    /**
     * 处理带有 @InjectPlugin 注解的类
     * Handle class with @InjectPlugin annotation
     */
    private void processPluginClass(Class<?> cls, InjectPlugin annotation, List<Plugin> plugins,
            PluginClassLoader classLoader)
    {
        try {
            // 验证类是否实现了Plugin接口
            // Validate class implements Plugin interface
            if (!Plugin.class.isAssignableFrom(cls)) {
                log.warn("Class {} has @InjectPlugin annotation but doesn't implement Plugin interface", cls.getName());
                return;
            }

            // 创建插件实例
            // Create plugin instance
            Plugin plugin = (Plugin) cls.getDeclaredConstructor().newInstance();

            // 设置插件元数据
            // Set plugin metadata
            String name = annotation.name().isEmpty() ? cls.getSimpleName() : annotation.name();

            plugin.setPluginClassLoader(classLoader);
            plugins.add(plugin);

            log.info("Found inject plugin: {} (version: {})", name, annotation.version());
        }
        catch (Exception e) {
            log.error("Failed to process plugin class: {}", cls.getName(), e);
        }
    }

    /**
     * 获取类名
     * Get class name
     */
    private String getClassName(Path baseDir, Path classFile)
    {
        // 获取相对路径
        // Get relative path
        String relativePath = baseDir.relativize(classFile).toString();

        // 处理路径分隔符和移除.class扩展名
        // Handle path separators and remove .class extension
        String className = relativePath.replace(File.separatorChar, '.');
        if (className.startsWith("test-classes.")) {
            className = className.substring("test-classes.".length());
        }
        return className.replace(".class", "");
    }
}
