package org.devlive.metaforge.plugin.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class PluginPathUtils
{
    private PluginPathUtils() {}

    // 项目根目录标志文件/目录
    // Project root markers
    private static final List<String> PROJECT_ROOT_MARKERS = Arrays.asList(
            "pom.xml",                // Maven项目标志
            "build.gradle",           // Gradle项目标志
            ".git",                   // Git仓库标志
            ".gitignore",             // Git配置标志
            ".idea",                  // IDEA项目标志
            "mvnw",                   // Maven包装器标志
            "gradlew"                 // Gradle包装器标志
    );

    // 模块目录标志
    // Module directory markers
    private static final List<String> MODULE_MARKERS = Arrays.asList(
            "src/main/java",
            "src/main/resources",
            "src/test/java",
            "target/classes",
            "build/classes"
    );

    /**
     * 查找项目根目录
     * Find project root directory
     *
     * @return 项目根目录的Path对象
     * Path object of project root directory
     */
    public static Path findProjectRoot()
    {
        Path rootPath;

        // 1. 首先尝试从类加载路径查找
        // First try to find from class loading path
        try {
            String className = PluginPathUtils.class.getName().replace('.', '/') + ".class";
            URL classUrl = PluginPathUtils.class.getClassLoader().getResource(className);

            if (classUrl != null) {
                String classPath = classUrl.getPath();
                // 处理JAR文件路径
                // Handle JAR file path
                if (classPath.contains(".jar!")) {
                    classPath = classPath.substring(0, classPath.indexOf(".jar!") + 4);
                }

                Path path = Paths.get(new File(classPath).toURI());
                log.debug("Starting search from class path: {}", path);
                rootPath = findRootFromPath(path);

                if (rootPath != null) {
                    log.info("Found project root from class path: {}", rootPath);
                    return rootPath;
                }
            }
        }
        catch (Exception e) {
            log.debug("Failed to find root from class path", e);
        }

        // 2. 尝试从当前工作目录查找
        // Try to find from current working directory
        try {
            Path currentPath = Paths.get("").toAbsolutePath();
            log.debug("Starting search from current directory: {}", currentPath);
            rootPath = findRootFromPath(currentPath);

            if (rootPath != null) {
                log.info("Found project root from current directory: {}", rootPath);
                return rootPath;
            }
        }
        catch (Exception e) {
            log.debug("Failed to find root from current directory", e);
        }

        // 3. 尝试从系统属性user.dir查找
        // Try to find from system property user.dir
        try {
            String userDir = System.getProperty("user.dir");
            if (userDir != null) {
                Path userPath = Paths.get(userDir);
                log.debug("Starting search from user.dir: {}", userPath);
                rootPath = findRootFromPath(userPath);

                if (rootPath != null) {
                    log.info("Found project root from user.dir: {}", rootPath);
                    return rootPath;
                }
            }
        }
        catch (Exception e) {
            log.debug("Failed to find root from user.dir", e);
        }

        // 4. 如果都找不到，向上遍历所有父目录
        // If not found, traverse all parent directories
        try {
            Path currentPath = Paths.get("").toAbsolutePath();
            while (currentPath != null && currentPath.getParent() != null) {
                if (isProjectRoot(currentPath)) {
                    log.info("Found project root from parent traversal: {}", currentPath);
                    return currentPath;
                }
                currentPath = currentPath.getParent();
            }
        }
        catch (Exception e) {
            log.debug("Failed to find root from parent traversal", e);
        }

        // 5. 最后返回当前目录作为后备方案
        // Finally return current directory as fallback
        Path fallback = Paths.get("").toAbsolutePath();
        log.warn("Could not find project root, using fallback: {}", fallback);
        return fallback;
    }

    /**
     * 从指定路径向上查找项目根目录
     * Find project root directory from specified path
     *
     * @param startPath 开始搜索的路径
     * path to start search from
     * @return 项目根目录路径，如果未找到返回 null
     * project root path, null if not found
     */
    private static Path findRootFromPath(Path startPath)
    {
        try {
            Path currentPath = startPath;
            while (currentPath != null && currentPath.getParent() != null) {
                // 如果当前目录是模块目录，继续向上查找
                // If current directory is a module directory, continue searching up
                if (isModuleDirectory(currentPath)) {
                    currentPath = currentPath.getParent();
                    continue;
                }

                // 检查是否是项目根目录
                // Check if it's project root directory
                if (isProjectRoot(currentPath)) {
                    return currentPath;
                }

                currentPath = currentPath.getParent();
            }
        }
        catch (Exception e) {
            log.debug("Error while searching for project root from path: {}", startPath, e);
        }
        return null;
    }

    /**
     * 检查给定路径是否为项目根目录
     * Check if given path is project root directory
     */
    private static boolean isProjectRoot(Path path)
    {
        try {
            // 检查是否存在任何根目录标志
            // Check if any root marker exists
            boolean hasRootMarker = PROJECT_ROOT_MARKERS.stream()
                    .anyMatch(marker -> Files.exists(path.resolve(marker)));

            if (!hasRootMarker) {
                return false;
            }

            // 额外检查是否有src目录或其他项目结构
            // Additional check for src directory or other project structure
            return Files.exists(path.resolve("src")) ||
                    Files.exists(path.resolve("pom.xml")) ||
                    Files.exists(path.resolve("build.gradle"));
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查给定路径是否为模块目录
     * Check if given path is module directory
     */
    private static boolean isModuleDirectory(Path path)
    {
        return MODULE_MARKERS.stream()
                .anyMatch(marker -> Files.exists(path.resolve(marker)));
    }

    /**
     * 解析插件路径
     * Resolve plugin path
     */
    public static Path resolvePluginPath(Path path)
    {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        // 如果是绝对路径，直接返回
        // If absolute path, return directly
        if (path.isAbsolute()) {
            log.debug("Using absolute path: {}", path);
            return path;
        }

        // 解析相对于项目根目录的路径
        // Resolve path relative to project root
        Path projectRoot = findProjectRoot();
        Path resolvedPath = projectRoot.resolve(path).normalize();
        log.debug("Resolved plugin path {} -> {}", path, resolvedPath);
        return resolvedPath;
    }

    /**
     * 从指定路径中查找编译输出目录
     * Find compilation output directory from specified path
     */
    public static Optional<Path> findOutputDirectory(Path projectPath)
    {
        try (Stream<Path> paths = Files.walk(projectPath, 3)) {
            return paths.filter(path -> path.endsWith("classes") ||
                            path.endsWith("resources"))
                    .filter(Files::exists)
                    .findFirst();
        }
        catch (IOException e) {
            log.debug("Failed to find output directory for path: {}", projectPath, e);
            return Optional.empty();
        }
    }

    /**
     * 安全地将子路径添加到项目根目录
     * Safely append a sub-path to the project root directory
     *
     * @param subPath 要添加的子路径
     * @return 组合后的路径
     */
    public static Path appendPath(String subPath)
    {
        Path root = findProjectRoot();
        String safePath = subPath.startsWith("/") ? subPath.substring(1) : subPath;
        return root.resolve(safePath);
    }
}
