package org.devlive.metaforge.plugin;

import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.devlive.metaforge.common.utils.DateUtils;
import org.devlive.metaforge.common.utils.EnvironmentUtils;
import org.devlive.metaforge.plugin.loader.PluginClassLoader;
import org.devlive.metaforge.plugin.loader.PluginLoaderFactory;
import org.devlive.metaforge.plugin.loader.PropertiesPluginLoader;
import org.devlive.metaforge.plugin.loader.TarPluginLoader;
import org.devlive.metaforge.plugin.utils.PluginClassLoaderUtils;
import org.devlive.metaforge.plugin.utils.PluginPathUtils;
import org.devlive.metaforge.plugin.utils.VersionUtils;

import javax.annotation.PreDestroy;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "EI_EXPOSE_REP2"})
public class PluginManager
{
    // 插件配置
    // Plugin configuration
    private final PluginConfigure config;

    // 插件存储映射
    // Plugin storage mapping
    private final Map<String, PluginMetadata> plugins;

    // 插件类加载器映射
    // Plugin class loader mapping
    private final Map<String, PluginClassLoader> pluginClassLoaders;

    // 运行状态标志
    // Running state flag
    @Getter
    private volatile boolean running;

    // 插件安装锁
    // Plugin installation lock
    private final Object installLock = new Object();

    // 临时目录前缀
    // Temporary directory prefix
    private static final String TEMP_DIR_PREFIX = "plugin_install_";

    // 默认版本
    // Default version
    private static final String DEFAULT_VERSION = "1.0.0";

    // 插件监视器
    // Plugin watcher
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "plugin-watcher-" + System.currentTimeMillis());
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> watcher;

    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Reject invalid configuration before any plugins are opened")
    public PluginManager(PluginConfigure config)
    {
        if (config.getLoadThreads() < 0) {
            throw new IllegalArgumentException("Plugin loading thread count must be zero or positive");
        }
        if (config.isAutoReload() && config.getScanInterval() <= 0) {
            throw new IllegalArgumentException("Plugin scan interval must be positive when auto reload is enabled");
        }
        this.config = config;
        this.plugins = Maps.newConcurrentMap();
        this.pluginClassLoaders = Maps.newConcurrentMap();
    }

    // 启动插件管理器
    // Start plugin manager
    public void start()
    {
        synchronized (installLock) {
            if (running) {
                return;
            }
            createPluginsDirectoryIfNotExists();
            loadPlugins();
            running = true;
            if (config.isAutoReload()) {
                startPluginWatcher();
            }
        }
    }

    // 停止插件管理器
    // Stop plugin manager
    public void stop()
    {
        synchronized (installLock) {
            running = false;
            if (watcher != null) {
                watcher.cancel(false);
                watcher = null;
            }
            List<PluginMetadata> loaded = new ArrayList<>(plugins.values());
            plugins.clear();
            loaded.forEach(this::closePluginClassLoader);
            pluginClassLoaders.clear();
        }
    }

    // 创建插件目录（如果不存在）
    // Create plugins directory if not exists
    private void createPluginsDirectoryIfNotExists()
    {
        if (!config.getPluginsDir().toFile().isFile()) {
            try {
                Files.createDirectories(config.getPluginsDir());
            }
            catch (IOException e) {
                log.warn("Failed to create plugins directory", e);
            }
        }
    }

    // 验证目录名称合法性
    // Validate directory name
    private boolean isValidDirectoryName(String name)
    {
        return name.matches("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");
    }

    // 安装新插件
    // Install new plugin
    public boolean installPlugin(Path sourcePath, String targetDirectory)
    {
        // 参数验证
        // Parameter validation
        if (sourcePath == null || targetDirectory == null || targetDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("Source path and target directory cannot be null or empty");
        }

        // 对于本地文件才验证存在性
        // Only verify existence for local files
        if (!sourcePath.toString().startsWith("http") && !sourcePath.toString().startsWith("https")) {
            if (!Files.exists(sourcePath)) {
                log.error("Source plugin path does not exist: {}", sourcePath);
                return false;
            }
        }

        // 验证目标目录名称合法性
        // Validate target directory name
        if (!isValidDirectoryName(targetDirectory)) {
            log.error("Invalid target directory name: {}", targetDirectory);
            return false;
        }

        synchronized (installLock) {
            Path tempDir = null;
            boolean installed;

            try {
                // 如果是IDE环境，或者插件目录是文件，则使用默认目录
                // Use default directory for IDE environment or if plugins directory is a file
                if (EnvironmentUtils.isIdeEnvironment() || Files.isRegularFile(config.getPluginsDir())) {
                    config.setPluginsDir(PluginPathUtils.appendPath("plugins"));
                }

                // 创建临时目录
                // Create temporary directory
                tempDir = Files.createTempDirectory(config.getPluginsDir(), TEMP_DIR_PREFIX);

                // 根据插件类型处理安装
                // Handle installation based on plugin type
                installed = processPluginInstallation(sourcePath, tempDir);

                if (installed) {
                    // 创建最终目标目录
                    // Create final target directory
                    Path targetPath = config.getPluginsDir().resolve(targetDirectory);

                    // 如果目标目录已存在，先备份
                    // Backup existing target directory if it exists
                    if (Files.exists(targetPath)) {
                        backupExistingPlugin(targetPath);
                    }

                    // 将临时目录移动到最终位置
                    // Move temporary directory to final location
                    moveDirectory(tempDir, targetPath);

                    // 加载新安装的插件
                    // Load newly installed plugin
                    loadPluginFromDirectory(targetPath);

                    log.info("Successfully installed plugin from {} to {}", sourcePath, targetPath);
                    return true;
                }

                return false;
            }
            catch (Exception e) {
                log.error("Failed to install plugin from: {} to {}", sourcePath, targetDirectory, e);
                return false;
            }
            finally {
                // 清理临时目录
                // Clean up temporary directory
                if (tempDir != null) {
                    deleteDirectory(tempDir);
                }
            }
        }
    }

    // 处理插件安装
    // Process plugin installation
    private boolean processPluginInstallation(Path sourcePath, Path tempDir)
            throws IOException
    {
        String extension = FilenameUtils.getExtension(sourcePath.toString()).toLowerCase();
        AtomicBoolean installed = new AtomicBoolean(false);

        // 检测并处理插件类型
        // Detect and handle plugin type
        try {
            // 检测并处理插件类型
            // Detect and handle plugin type
            if ("tar".equals(extension)
                    || "tar.gz".equals(extension)
                    || "tgz".equals(extension)
                    || "gz".equals(extension)
            ) {
                installed.set(installTarPlugin(sourcePath, tempDir));
            }
            // Properties 插件
            // Properties plugin
            if ("properties".equals(extension)) {
                installed.set(installPropertiesPlugin(sourcePath, tempDir));
            }
            // POM 插件
            // POM plugin
            else if (isPomPlugin(sourcePath)) {
                installed.set(installPomPlugin(sourcePath, tempDir));
            }
            // 编译后的插件
            // Compiled plugin
            else if ("class".equals(extension) || "jar".equals(extension)) {
                installed.set(installCompiledPlugin(sourcePath, tempDir));
            }
            // 目录类型插件
            // Directory plugin
            else if (Files.isDirectory(sourcePath)) {
                installed.set(installDirectoryPlugin(sourcePath, tempDir));
            }
            // SPI 类型插件
            // SPI plugin
            else if (isSpiPlugin(sourcePath)) {
                installed.set(installSpiPlugin(sourcePath, tempDir));
            }
            else {
                log.warn("Unsupported plugin type for: {}", sourcePath);
            }

            // 验证安装后的插件结构
            // Validate installed plugin structure
            if (installed.get()) {
                validatePluginStructure(tempDir);
            }
        }
        catch (Exception e) {
            log.error("Failed to process plugin installation: {}", sourcePath, e);
            throw e;
        }

        return installed.get();
    }

    // 备份现有插件
    // Backup existing plugin
    private void backupExistingPlugin(Path pluginPath)
            throws IOException
    {
        if (Files.exists(pluginPath)) {
            String timestamp = DateUtils.formatYMDHMS();
            Path backupPath = pluginPath.getParent().resolve(pluginPath.getFileName() + ".backup." + timestamp);

            try {
                Files.move(pluginPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Created backup of existing plugin: {}", backupPath);
            }
            catch (IOException e) {
                log.error("Failed to create backup of existing plugin: {}", pluginPath, e);
                throw e;
            }
        }
    }

    // 移动目录
    // Move directory
    private void moveDirectory(Path source, Path target)
            throws IOException
    {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (FileAlreadyExistsException e) {
            // 如果移动失败，尝试复制
            // If move fails, try copy
            copyDirectory(source, target);
        }
    }

    // 删除目录
    // Delete directory
    private void deleteDirectory(Path directory)
    {
        try {
            if (Files.exists(directory)) {
                try (Stream<Path> pathStream = Files.walk(directory)) {
                    pathStream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(file -> {
                                if (!file.delete()) {
                                    log.warn("Failed to delete file: {}", file.getAbsolutePath());
                                    if (file.exists()) {
                                        file.deleteOnExit();
                                    }
                                }
                            });
                }
            }
        }
        catch (IOException e) {
            log.warn("Failed to delete directory: {}", directory, e);
        }
    }

    // 验证插件结构
    // Validate plugin structure
    private void validatePluginStructure(Path pluginPath)
            throws IOException
    {
        // 验证基本文件结构
        // Validate basic file structure
        if (!Files.exists(pluginPath)) {
            throw new IOException("Plugin path does not exist: " + pluginPath);
        }

        boolean hasRequiredFiles = Files.exists(pluginPath.resolve("pom.xml")) ||
                Files.exists(pluginPath.resolve("plugin.properties")) ||
                Files.exists(pluginPath.resolve("META-INF/services"));

        if (hasRequiredFiles) {
            return;
        }

        // 检查是否存在 class 或 jar 文件
        // Check for class or jar files
        try (Stream<Path> paths = Files.list(pluginPath)) {
            boolean hasClassOrJar = paths.anyMatch(path -> {
                String pathStr = path.toString();
                return pathStr.endsWith(".class") || pathStr.endsWith(".jar");
            });

            if (!hasClassOrJar) {
                throw new IOException("Invalid plugin structure in: " + pluginPath);
            }
        }
    }

    // 检查是否为 POM 插件
    // Check if it's a POM plugin
    private boolean isPomPlugin(Path path)
    {
        return path.toString().endsWith("pom.xml") ||
                (Files.isDirectory(path) && Files.exists(path.resolve("pom.xml")));
    }

    // 检查是否为 SPI 插件
    // Check if it's SPI plugin
    private boolean isSpiPlugin(Path path)
    {
        if (!Files.isDirectory(path)) {
            return false;
        }

        Path servicesPath = path.resolve("META-INF/services");
        if (!Files.exists(servicesPath)) {
            return false;
        }

        try (Stream<Path> serviceFiles = Files.list(servicesPath)) {
            return serviceFiles.anyMatch(file -> file.toString().endsWith(Plugin.class.getName()));
        }
        catch (IOException e) {
            log.debug("Failed to check SPI plugin at path: {}", path, e);
            return false;
        }
    }

    // 安装 Properties 类型插件
    // Install Properties type plugin
    private boolean installPropertiesPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        Files.copy(sourcePath, targetPath.resolve("plugin.properties"),
                StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    // 安装 POM 类型插件
    // Install POM type plugin
    private boolean installPomPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        if (Files.isDirectory(sourcePath)) {
            copyDirectory(sourcePath, targetPath);
        }
        else {
            Files.copy(sourcePath, targetPath.resolve("pom.xml"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    // 安装编译后的插件
    // Install compiled plugin
    private boolean installCompiledPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        if (Files.isDirectory(sourcePath)) {
            copyDirectory(sourcePath, targetPath);
        }
        else {
            Files.copy(sourcePath, targetPath.resolve(sourcePath.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    // 安装目录类型插件
    // Install directory type plugin
    private boolean installDirectoryPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        copyDirectory(sourcePath, targetPath);
        return true;
    }

    // 安装 SPI 类型插件
    // Install SPI type plugin
    private boolean installSpiPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        copyDirectory(sourcePath, targetPath);
        return true;
    }

    // 安装 Tar 类型插件
    // Install Tar type plugin
    private boolean installTarPlugin(Path sourcePath, Path tempDir)
            throws IOException
    {
        // 直接使用 TarPluginLoader 处理
        // Directly use TarPluginLoader to handle
        TarPluginLoader tarPluginLoader = new TarPluginLoader();
        // 先加载插件到临时目录
        // Load plugins to temporary directory
        List<Plugin> plugins = tarPluginLoader.load(sourcePath, tempDir, config.getParentClassLoaderPackages());
        if (!plugins.isEmpty()) {
            // 查找解压后的子目录
            // Find extracted subdirectory
            try (Stream<Path> paths = Files.list(tempDir)) {
                Optional<Path> subDir = paths
                        .filter(Files::isDirectory)
                        .findFirst();

                if (subDir.isPresent()) {
                    // 如果存在子目录，将其内容移动到临时目录根目录
                    // If subdirectory exists, move its contents to temp directory root
                    Path source = subDir.get();
                    try (Stream<Path> files = Files.list(source)) {
                        files.forEach(file -> {
                            try {
                                Path target = tempDir.resolve(file.getFileName());
                                Files.move(file, target);
                            }
                            catch (IOException e) {
                                log.error("Failed to move file: {} to {}", file, tempDir, e);
                            }
                        });
                    }
                    // 删除空的子目录
                    // Delete empty subdirectory
                    Files.delete(source);
                }
            }
            return true;
        }
        return false;
    }

    // 复制目录
    // Copy directory
    private void copyDirectory(Path source, Path target)
            throws IOException
    {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    }
                    else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                catch (IOException e) {
                    log.error("Failed to copy file: {} to {}", sourcePath, target, e);
                }
            });
        }
    }

    // 先发现独立路径，再并行初始化，最后按稳定顺序注册
    // Discover independent paths, initialize in parallel, then register in stable order
    private void loadPlugins()
    {
        synchronized (installLock) {
            long started = System.nanoTime();
            List<Path> paths;
            try {
                paths = discoverPluginPaths();
            }
            catch (IOException e) {
                log.error("Failed to scan plugins directory", e);
                return;
            }
            if (paths.isEmpty()) {
                log.info("No plugin paths found in {}", config.getPluginsDir());
                return;
            }

            int configured = config.getLoadThreads();
            int automatic = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors()));
            int workers = Math.min(paths.size(), configured == 0 ? automatic : configured);
            log.info("Loading {} plugin paths with {} worker(s)", paths.size(), workers);
            AtomicInteger threadNumber = new AtomicInteger();
            ExecutorService executor = Executors.newFixedThreadPool(workers, task -> {
                Thread thread = new Thread(task, "plugin-loader-" + threadNumber.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
            List<Future<List<PluginMetadata>>> futures = new ArrayList<>();
            List<PluginMetadata> staged = new ArrayList<>();
            AtomicBoolean aborted = new AtomicBoolean();
            boolean completed = false;
            try {
                for (Path path : paths) {
                    futures.add(executor.submit(() -> {
                        List<PluginMetadata> loaded = preparePlugins(path);
                        synchronized (staged) {
                            if (aborted.get()) {
                                loaded.forEach(this::closePluginClassLoader);
                            }
                            else {
                                staged.addAll(loaded);
                            }
                        }
                        return loaded;
                    }));
                }
                List<PluginMetadata> ordered = new ArrayList<>();
                int failedPaths = 0;
                for (int index = 0; index < futures.size(); index++) {
                    try {
                        List<PluginMetadata> loaded = futures.get(index).get();
                        ordered.addAll(loaded);
                        if (loaded.isEmpty()) {
                            failedPaths++;
                        }
                    }
                    catch (ExecutionException e) {
                        if (e.getCause() instanceof VirtualMachineError) {
                            throw (VirtualMachineError) e.getCause();
                        }
                        failedPaths++;
                        log.error("Failed to load plugin path: {}", paths.get(index), e.getCause());
                    }
                }
                ordered.forEach(this::registerPlugin);
                completed = true;
                log.info("Plugin loading completed: {} registered plugin(s), {} failed/empty path(s), {} worker(s), {} ms",
                        plugins.size(), failedPaths, workers, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while loading plugins", e);
            }
            finally {
                if (!completed) {
                    synchronized (staged) {
                        aborted.set(true);
                        staged.forEach(this::closePluginClassLoader);
                    }
                    futures.forEach(future -> future.cancel(true));
                    executor.shutdownNow();
                }
                else {
                    executor.shutdown();
                }
            }
        }
    }

    private List<Path> discoverPluginPaths()
            throws IOException
    {
        Path root = config.getPluginsDir().toAbsolutePath().normalize();
        List<Path> candidates;
        if (Files.isRegularFile(root)) {
            candidates = List.of(root);
        }
        else {
            try (Stream<Path> paths = Files.walk(root, config.getScanDepth() == 0 ? 1 : config.getScanDepth())) {
                candidates = paths.filter(Files::isDirectory)
                        .filter(path -> !path.equals(root))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }
        Set<Path> plugins = new LinkedHashSet<>();
        Set<Path> manifests = new HashSet<>();
        for (Path path : candidates) {
            try {
                expandPluginPath(path, plugins, manifests);
            }
            catch (IOException e) {
                log.warn("Failed to resolve plugin path: {}", path, e);
            }
        }
        return new ArrayList<>(plugins);
    }

    private void expandPluginPath(Path path, Set<Path> paths, Set<Path> manifests)
            throws IOException
    {
        Path normalized = Files.exists(path) ? path.toRealPath() : path.toAbsolutePath().normalize();
        if (Files.isRegularFile(normalized) && normalized.toString().endsWith(".properties")) {
            if (!manifests.add(normalized)) {
                log.warn("Skipping repeated plugin manifest: {}", normalized);
                return;
            }
            for (Path entry : new PropertiesPluginLoader().resolvePaths(normalized)) {
                try {
                    expandPluginPath(entry, paths, manifests);
                }
                catch (IOException e) {
                    log.warn("Failed to resolve manifest entry: {}", entry, e);
                }
            }
        }
        else {
            if (Files.isRegularFile(normalized) && normalized.endsWith("pom.xml")) {
                normalized = normalized.getParent();
            }
            paths.add(normalized);
        }
    }

    // 安装时仍同步加载；启动/监视扫描复用同一初始化过程
    // Installation remains synchronous; startup and watcher scans share preparation
    private void loadPluginFromDirectory(Path pluginDir)
    {
        preparePlugins(pluginDir).forEach(this::registerPlugin);
    }

    private List<PluginMetadata> preparePlugins(Path pluginDir)
    {
        long started = System.nanoTime();
        List<PluginMetadata> prepared = new ArrayList<>();
        Set<PluginClassLoader> loaders = new HashSet<>();
        try {
            List<Plugin> modules = PluginLoaderFactory.loadPlugins(pluginDir, config.getParentClassLoaderPackages());
            PluginClassLoader sharedFallback = null;
            for (Plugin module : modules) {
                try {
                    // 复用加载器已建立的真实依赖路径，不再提前重复扫描整个插件目录
                    // Reuse the actual module loader instead of scanning all dependencies again
                    PluginClassLoader loader = module.getPluginClassLoader();
                    if (loader == null) {
                        if (config.isShareClassLoaderWhenSameDir() && sharedFallback != null) {
                            loader = sharedFallback;
                        }
                        else {
                            loader = PluginClassLoaderUtils.createClassLoader(pluginDir, pluginDir.getFileName().toString(),
                                    VersionUtils.determinePluginVersion(pluginDir), true, config.getParentClassLoaderPackages());
                            sharedFallback = loader;
                        }
                        module.setPluginClassLoader(loader);
                    }
                    loaders.add(loader);
                    PluginClassLoader actualLoader = loader;
                    prepared.add(PluginContextManager.runWithClassLoader(actualLoader, () -> {
                        Injector injector = Guice.createInjector(module);
                        module.setInjector(injector);
                        String version = module.getVersion();
                        return PluginMetadata.builder()
                                .name(module.getName())
                                .version(Objects.equals(version, DEFAULT_VERSION) ? actualLoader.getPluginVersion() : version)
                                .location(pluginDir)
                                .state(PluginState.CREATED)
                                .classLoader(actualLoader)
                                .loaderName(module.getClassLoader())
                                .instance(module)
                                .type(module.getType())
                                .key(module.getKey())
                                .configure(module.describe())
                                .loadTimestamp(System.currentTimeMillis())
                                .loadTime(DateUtils.formatYMDHMSWithInterval())
                                .build();
                    }));
                }
                catch (Exception | LinkageError e) {
                    log.error("Failed to initialize plugin module: {} from {}", module.getClass().getName(), pluginDir, e);
                }
            }
        }
        catch (Exception | LinkageError e) {
            log.error("Failed to load plugin from directory: {}", pluginDir, e);
        }
        finally {
            prepared.forEach(metadata -> loaders.remove(metadata.getClassLoader()));
            loaders.forEach(this::closeLoader);
            log.info("Prepared {} plugin(s) from {} in {} ms", prepared.size(), pluginDir,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        }
        return prepared;
    }

    private void registerPlugin(PluginMetadata metadata)
    {
        synchronized (installLock) {
            String name = metadata.getName();
            PluginMetadata oldPlugin = plugins.remove(name);
            if (oldPlugin != null && oldPlugin.getClassLoader() != metadata.getClassLoader()) {
                closePluginClassLoader(oldPlugin);
            }
            PluginClassLoader loader = (PluginClassLoader) metadata.getClassLoader();
            pluginClassLoaders.put(name, loader);
            if (config.isShareClassLoaderWhenSameDir()) {
                pluginClassLoaders.put(metadata.getLocation().toString(), loader);
            }
            plugins.put(name, metadata);
            log.info("Install plugin: [ {} ] type [ {} ] version [ {} ] loader [ {} ] from source [ {} ]",
                    name, metadata.getType().getName(), metadata.getVersion(), metadata.getLoaderName(), metadata.getLocation());
        }
    }

    private void closePluginClassLoader(PluginMetadata metadata)
    {
        ClassLoader loader = metadata.getClassLoader();
        pluginClassLoaders.remove(metadata.getName(), loader);
        // 同目录的其他模块仍在使用时，保留共享加载器
        // Keep shared loaders alive while another registered module still uses them
        if (plugins.values().stream().noneMatch(other -> other != metadata && other.getClassLoader() == loader)) {
            pluginClassLoaders.values().removeIf(value -> value == loader);
            if (loader instanceof PluginClassLoader) {
                closeLoader((PluginClassLoader) loader);
            }
        }
    }

    private void closeLoader(PluginClassLoader loader)
    {
        try {
            loader.close();
        }
        catch (IOException e) {
            log.warn("Failed to close plugin class loader: {}", loader.getPluginName(), e);
        }
    }

    // 启动插件监视器线程
    // Start plugin watcher thread
    private void startPluginWatcher()
    {
        watcher = scheduler.scheduleWithFixedDelay(() -> {
            try {
                synchronized (installLock) {
                    if (running) {
                        loadPlugins();
                    }
                }
            }
            catch (Exception e) {
                log.error("Failed to load plugins during watch cycle", e);
            }
        }, config.getScanInterval(), config.getScanInterval(), TimeUnit.MILLISECONDS);

        log.info("Started plugin watcher with scan interval: {}ms", config.getScanInterval());
    }

    // 停止插件监视器线程
    // Stop plugin watcher thread
    private void stopPluginWatcher()
    {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(config.getScanInterval(), TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    log.warn("Plugin watcher scheduler did not terminate");
                }
            }
        }
        catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("Interrupted while shutting down plugin watcher", e);
        }
    }

    @PreDestroy
    public void destroy()
    {
        stopPluginWatcher();
        stop();
    }

    // 获取指定名称的插件
    // Get plugin by name
    public Optional<Plugin> getPlugin(String name)
    {
        return Optional.ofNullable(plugins.get(name))
                .map(info -> (Plugin) info.getInstance());
    }

    // 获取插件的类加载器
    // Get plugin's class loader
    public Optional<ClassLoader> getPluginClassLoader(String pluginName)
    {
        return Optional.ofNullable(pluginClassLoaders.get(pluginName));
    }

    // 获取当前已加载的所有插件类加载器
    // Get all currently loaded plugin class loaders
    public Map<String, PluginClassLoader> getPluginClassLoaders()
    {
        return Maps.newHashMap(pluginClassLoaders);
    }

    // 获取所有插件信息
    // Get all plugin information
    public List<PluginMetadata> getPluginInfos()
    {
        return new ArrayList<>(plugins.values());
    }

    // 卸载指定名称的插件
    // Unload plugin by name
    public boolean uninstallPlugin(String name)
    {
        if (name == null || name.trim().isEmpty()) {
            log.warn("Plugin name cannot be null or empty");
            return false;
        }

        synchronized (installLock) {
            try {
                // 获取插件元数据
                // Get plugin metadata
                PluginMetadata pluginMetadata = plugins.get(name);
                if (pluginMetadata == null) {
                    log.warn("Plugin not found: {}", name);
                    return false;
                }

                // 检查插件状态
                // Check plugin state
                if (pluginMetadata.getState() == PluginState.UNLOADED) {
                    log.warn("Plugin {} is already unloaded", name);
                    return false;
                }

                // 创建卸载备份
                // Create unload backup
                Path pluginLocation = pluginMetadata.getLocation();
                if (pluginLocation != null && Files.exists(pluginLocation)) {
                    createUnloadBackup(pluginLocation);
                }

                // 清理插件资源
                // Clean up plugin resources
                try {
                    // 关闭类加载器
                    // Close class loader
                    closePluginClassLoader(pluginMetadata);

                    // 清理注入器
                    // Clean up injector
                    Plugin plugin = (Plugin) pluginMetadata.getInstance();
                    if (plugin != null && plugin.getInjector() != null) {
                        // Close any resources managed by the injector
                        try {
                            plugin.getInjector().getInstance(AutoCloseable.class);
                        }
                        catch (Exception ignored) {
                            // Ignore if no AutoCloseable is bound
                        }
                    }

                    // 更新插件状态
                    // Update plugin state
                    pluginMetadata.setState(PluginState.UNLOADED);

                    // 从插件映射中移除
                    // Remove from plugin mapping
                    plugins.remove(name);

                    // 清理插件文件（如果配置了自动清理）
                    // Clean up plugin files (if auto cleanup is configured)
                    if (config.isAutoCleanup()) {
                        deletePluginFiles(pluginLocation);
                    }

                    log.info("Successfully unloaded plugin: {}", name);
                    return true;
                }
                catch (Exception e) {
                    log.error("Error while unloading plugin: {}", name, e);
                    // 尝试强制移除
                    // Try force removal
                    plugins.remove(name);
                    return false;
                }
            }
            catch (Exception e) {
                log.error("Failed to unload plugin: {}", name, e);
                return false;
            }
        }
    }

    // 创建卸载备份
    // Create unload backup
    private void createUnloadBackup(Path pluginLocation)
    {
        try {
            String timestamp = DateUtils.formatYMDHMS();
            Path backupPath = pluginLocation.getParent()
                    .resolve(pluginLocation.getFileName() + ".unload." + timestamp);

            if (Files.isDirectory(pluginLocation)) {
                copyDirectory(pluginLocation, backupPath);
            }
            else {
                Files.copy(pluginLocation, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Created unload backup at: {}", backupPath);
        }
        catch (IOException e) {
            log.warn("Failed to create unload backup for: {}", pluginLocation, e);
        }
    }

    private void deletePluginFiles(Path pluginLocation)
    {
        if (pluginLocation == null || !Files.exists(pluginLocation)) {
            return;
        }

        try {
            // 删除插件目录或文件
            // Delete plugin directory or file
            if (Files.isDirectory(pluginLocation)) {
                deleteDirectory(pluginLocation);
            }
            else {
                // 如果非IDE环境，或者插件目录是文件，才进行删除
                // Delete only if not in IDE environment, or if the plugin directory is a file
                if (!EnvironmentUtils.isIdeEnvironment() || !Files.isRegularFile(config.getPluginsDir())) {
                    Files.delete(pluginLocation);
                }
            }

            // 删除备份文件
            // Delete backup files
            Path parentDir = pluginLocation.getParent();
            String baseName = pluginLocation.getFileName().toString();
            try (Stream<Path> paths = Files.list(parentDir)) {
                paths.filter(path -> {
                            String fileName = path.getFileName().toString();
                            // 匹配 .backup.* 和 .unload.* 格式的备份文件
                            // Match backup files with .backup.* and .unload.* format
                            return fileName.startsWith(baseName + ".backup.") || fileName.startsWith(baseName + ".unload.");
                        })
                        .forEach(backupPath -> {
                            try {
                                if (Files.isDirectory(backupPath)) {
                                    deleteDirectory(backupPath);
                                }
                                else {
                                    Files.delete(backupPath);
                                }
                                log.info("Deleted backup file: {}", backupPath);
                            }
                            catch (IOException e) {
                                log.warn("Failed to delete backup file: {}", backupPath, e);
                            }
                        });
            }
            log.info("Deleted plugin files at: {}", pluginLocation);
        }
        catch (IOException e) {
            log.warn("Failed to delete plugin files at: {}", pluginLocation, e);
        }
    }

    // 强制卸载插件
    // Force unload plugin
    public boolean forceUnloadPlugin(String name)
    {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        synchronized (installLock) {
            try {
                PluginMetadata pluginMetadata = plugins.remove(name);
                if (pluginMetadata != null) {
                    try {
                        closePluginClassLoader(pluginMetadata);
                    }
                    catch (Exception e) {
                        log.warn("Error while force closing plugin classloader: {}", name, e);
                    }

                    try {
                        if (config.isAutoCleanup()) {
                            deletePluginFiles(pluginMetadata.getLocation());
                        }
                    }
                    catch (Exception e) {
                        log.warn("Error while force deleting plugin files: {}", name, e);
                    }

                    log.info("Force unloaded plugin: {}", name);
                    return true;
                }
                return false;
            }
            catch (Exception e) {
                log.error("Failed to force unload plugin: {}", name, e);
                return false;
            }
        }
    }
}
