package org.devlive.metaforge.plugin.loader;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.SpiType;
import org.devlive.metaforge.plugin.utils.VersionUtils;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class CompiledPomPluginLoader
        implements PluginLoader
{
    @Override
    public SpiType getType()
    {
        return SpiType.COMPILED_POM;
    }

    @Override
    public List<Plugin> load(Path path, Set<String> parentClassLoaderPackages)
    {
        try {
            // 处理传入的是pom.xml文件的情况
            // Handle the case when input is pom.xml file
            Path pomFile;
            if (path.toString().endsWith("pom.xml")) {
                pomFile = path;
                path = path.getParent();
            }
            else {
                pomFile = path.resolve("pom.xml");
            }

            if (!Files.exists(pomFile)) {
                log.debug("No pom.xml found in {}", path);
                return List.of();
            }

            // 获取编译后的类路径
            // Get compiled classpath
            Path targetClasses = path.resolve("target/classes");
            Path targetDependencies = path.resolve("target/dependency");

            // 如果已编译的类不存在，直接返回空列表
            // If compiled classes don't exist, return empty list
            if (!Files.exists(targetClasses)) {
                log.debug("Target classes directory not found: {}", targetClasses);
                return List.of();
            }

            // 创建类加载器
            // Create class loader
            PluginClassLoader classLoader = createProjectClassLoader(targetClasses, targetDependencies, path, parentClassLoaderPackages);

            // 查找并加载插件类
            // Find and load plugin classes
            return findAndLoadPlugins(classLoader, targetClasses);
        }
        catch (Exception e) {
            log.error("Failed to load compiled plugin from: {}", path, e);
            return List.of();
        }
    }

    // 创建项目类加载器
    // Create project class loader
    private PluginClassLoader createProjectClassLoader(Path targetClasses, Path targetDependencies, Path module, Set<String> parentClassLoaderPackages)
            throws Exception
    {
        List<URL> urls = new ArrayList<>();

        // 添加编译后的类路径
        // Add compiled classes path
        log.debug("Adding classes directory to classpath: {}", targetClasses);
        urls.add(targetClasses.toUri().toURL());

        // 添加所有依赖jar（如果存在）
        // Add all dependency jars (if exist)
        if (Files.exists(targetDependencies)) {
            log.debug("Adding dependencies from: {}", targetDependencies);
            try (Stream<Path> paths = Files.walk(targetDependencies)) {
                paths.filter(path -> path.toString().endsWith(".jar"))
                        .forEach(path -> {
                            try {
                                log.debug("Adding dependency to classpath: {}", path);
                                urls.add(path.toUri().toURL());
                            }
                            catch (Exception e) {
                                log.error("Failed to add dependency jar to classpath: {}", path, e);
                            }
                        });
            }
        }
        else {
            log.debug("Dependencies directory not found: {}", targetDependencies);
        }

        return new PluginClassLoader(
                urls.toArray(new URL[0]),
                getClass().getClassLoader(),
                module.getFileName().toString(),
                VersionUtils.determinePluginVersion(module),
                true,
                parentClassLoaderPackages
        );
    }

    // 查找并加载插件类
    // Find and load plugin classes
    private List<Plugin> findAndLoadPlugins(PluginClassLoader classLoader, Path targetClasses)
    {
        List<Plugin> plugins = new ArrayList<>();
        try {
            // 扫描编译后的类文件
            // Scan compiled class files
            try (Stream<Path> paths = Files.walk(targetClasses)) {
                paths.filter(path -> path.toString().endsWith(".class"))
                        .filter(path -> !path.toString().contains("$"))
                        .forEach(path -> {
                            try {
                                String className = getClassName(targetClasses, path);
                                Class<?> cls = classLoader.loadClass(className);

                                // 检查是否是具体的插件类
                                // Check if it's a concrete plugin class
                                if (Plugin.class.isAssignableFrom(cls) &&
                                        !cls.isInterface() &&
                                        !Modifier.isAbstract(cls.getModifiers())) {
                                    Plugin plugin = (Plugin) cls.getDeclaredConstructor().newInstance();
                                    plugin.setPluginClassLoader(classLoader);
                                    plugins.add(plugin);
                                    log.info("Loaded plugin class: {}", className);
                                }
                            }
                            catch (Exception e) {
                                log.debug("Failed to load class: {}", path, e);
                            }
                        });
            }
        }
        catch (Exception e) {
            log.error("Failed to scan for plugin classes", e);
        }
        return plugins;
    }

    // 获取类名
    // Get class name
    private String getClassName(Path baseDir, Path classFile)
    {
        String relativePath = baseDir.relativize(classFile).toString();
        return relativePath.replace(File.separatorChar, '.')
                .replace(".class", "");
    }
}
