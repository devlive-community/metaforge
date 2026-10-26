package org.devlive.metaforge.plugin.loader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.SpiType;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressFBWarnings(value = {"MS_OOI_PKGPROTECT", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
public interface PluginLoader
{
    Logger log = org.slf4j.LoggerFactory.getLogger(PluginLoader.class);

    Set<String> EXCLUDED_DIRS = new HashSet<>()
    {{
        // Maven 相关目录
        // Maven related directories
        add("target/test-classes");
        add("target/generated-sources");
        add("target/generated-test-sources");
        add("target/maven-status");
        add("target/maven-archiver");
        add("target/surefire-reports");
        add("target/reports");
        add("target/javadoc-bundle-options");

        // 版本控制和 IDE 目录
        // Version control and IDE directories
        add(".git");
        add(".idea");
        add(".vscode");
        add(".settings");
        add(".project");
        add(".classpath");

        // 依赖目录
        // Dependency directories
        add("node_modules");

        // SPI 特有的排除目录
        // SPI-specific exclusion directories
        add("META-INF/services/test");
        add("META-INF/maven");
        add("META-INF/versions");

        // 源码目录
        // Source code directory
        add("src/main/java");
        add("src/main/kotlin");
        add("src/main/resources");
        add("src/test");
        add("src/test/java");
        add("src/test/kotlin");
        add("src/test/resources");
    }};

    /**
     * 检查路径是否应该被排除
     * Check if the path should be excluded
     */
    default boolean isExcludedPath(Path path)
    {
        String pathStr = path.toString();
        return EXCLUDED_DIRS.stream()
                .anyMatch(dir -> pathStr.contains(dir.replace('/', File.separatorChar)));
    }

    /**
     * 检查目录是否有效且不在排除列表中
     * Check if the directory is valid and not in the exclusion list
     */
    default boolean isValidDirectory(Path path)
    {
        return Files.exists(path) &&
                Files.isDirectory(path) &&
                !isExcludedPath(path);
    }

    default String extractPluginName(Path path)
    {
        try {
            // 如果路径以target结尾，获取父目录名称作为插件名
            // If the path ends with target, use the parent directory name as the plugin name
            String fileName = path.getFileName().toString();
            if (fileName.equals("target")) {
                return path.getParent().getFileName().toString();
            }

            // 如果是在plugins目录下，直接使用目录名
            // If it's in the plugins directory, use the directory name
            Path parent = path.getParent();
            if (parent != null && parent.getFileName() != null &&
                    parent.getFileName().toString().equals("plugins")) {
                return fileName;
            }

            // 尝试从完整路径中找到正确的插件目录名
            // 通常是最后一个非target的目录名
            // Try to find the correct plugin directory name from the full path
            // Usually the last non-target directory name
            Path current = path;
            while (current.getParent() != null) {
                String name = current.getFileName().toString();
                if (!name.equals("target") && !name.equals("plugins")) {
                    return name;
                }
                current = current.getParent();
            }

            // 如果上述都没找到，返回原始文件名
            return fileName;
        }
        catch (Exception e) {
            log.warn("Error extracting plugin name from path: {}, using fallback", path, e);
            return path.getFileName().toString();
        }
    }

    // 获取加载器类型
    // Get loader type
    SpiType getType();

    // 加载插件
    // Load plugins
    List<Plugin> load(Path path, Set<String> parentClassLoaderPackages);
}
