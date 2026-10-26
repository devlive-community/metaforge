package org.devlive.metaforge.plugin.loader;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginContextManager;
import org.devlive.metaforge.plugin.SpiType;
import org.devlive.metaforge.plugin.utils.PluginClassLoaderUtils;
import org.devlive.metaforge.plugin.utils.VersionUtils;

import javax.net.ssl.SSLContext;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

/**
 * Tar 格式插件加载器，支持从本地文件系统或网络 URL 加载插件
 * Tar format plugin loader, supports loading plugins from local filesystem or network URL
 */
@Slf4j
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
public class TarPluginLoader
        implements PluginLoader
{
    // 临时目录前缀
    // Temporary directory prefix
    private static final String TEMP_DIR_PREFIX = "plugin_";

    /**
     * 获取加载器类型
     * Get loader type
     */
    @Override
    public SpiType getType()
    {
        return SpiType.TAR;
    }

    /**
     * 加载插件，支持本地路径和网络 URL
     * Load plugins, supports local path and network URL
     *
     * @param path 插件路径或 URL
     * Plugin path or URL
     * @return 加载的插件列表
     * List of loaded plugins
     */
    @Override
    public List<Plugin> load(Path path, Set<String> parentClassLoaderPackages)
    {
        return load(path, null, parentClassLoaderPackages);
    }

    /**
     * 加载插件到指定目录
     * Load plugins to specified directory
     *
     * @param path 插件路径或 URL
     * Plugin path or URL
     * @param targetDir 目标解压目录，如果为null则使用临时目录
     * Target extraction directory, use temporary directory if null
     * @return 加载的插件列表
     * List of loaded plugins
     */
    public List<Plugin> load(Path path, Path targetDir, Set<String> parentClassLoaderPackages)
    {
        try {
            if (isValidTarPath(path)) {
                // 如果是 URL 路径，先下载到本地
                // If it's a URL path, download it first
                if (path.toString().startsWith("http") || path.toString().startsWith("https")) {
                    path = downloadTarFile(path.toString().replace(":/", "://"));
                }

                if (isValidTarFile(path)) {
                    // 使用指定的目录或创建临时目录
                    // Use specified directory or create temporary directory
                    Path extractDir = targetDir != null ? targetDir : createTempDirectory();

                    // 解压 tar 文件
                    // Extract tar file
                    extractTarFile(path, extractDir);

                    // 从解压目录加载插件
                    // Load plugins from extracted directory
                    List<Plugin> plugins = loadPluginsFromDirectory(extractDir, parentClassLoaderPackages);

                    // 如果使用的是临时目录，则清理
                    // Clean up if using temporary directory
                    if (targetDir == null) {
                        cleanupTempDirectory(extractDir);
                    }

                    return plugins;
                }
            }

            return List.of();
        }
        catch (Exception e) {
            log.error("Failed to load plugins from tar file: {}", path, e);
            return List.of();
        }
    }

    /**
     * 从解压目录加载插件
     * Load plugins from extracted directory
     *
     * @param directory 插件目录
     * Plugin directory
     * @return 加载的插件列表
     * List of loaded plugins
     */
    private List<Plugin> loadPluginsFromDirectory(Path directory, Set<String> parentClassLoaderPackages)
            throws Exception
    {
        List<Plugin> allPlugins = new ArrayList<>();

        // 遍历目录查找插件
        // Traverse directory to find plugins
        try (Stream<Path> pathStream = Files.walk(directory)) {
            pathStream.filter(Files::isDirectory)
                    .filter(this::isValidDirectory)
                    .forEach(pluginDir -> {
                        try {
                            // 获取插件名称和版本
                            // Get plugin name and version
                            String pluginName = pluginDir.getFileName().toString();
                            String version = VersionUtils.determinePluginVersion(pluginDir);

                            // 创建插件专用类加载器
                            // Create plugin-specific class loader
                            PluginClassLoader classLoader = PluginClassLoaderUtils.createClassLoader(
                                    pluginDir,
                                    pluginName,
                                    version,
                                    true,
                                    parentClassLoaderPackages
                            );

                            // 在插件类加载器上下文中加载插件
                            // Load plugins in plugin class loader context
                            List<Plugin> plugins = PluginContextManager.runWithClassLoader(classLoader, () -> {
                                ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
                                return StreamSupport.stream(serviceLoader.spliterator(), false)
                                        .filter(plugin -> {
                                            // 检查插件类是否来自排除目录
                                            // Check if the plugin class is from an excluded directory
                                            String className = plugin.getClass().getName();
                                            Path classPath = Path.of(className.replace('.', File.separatorChar) + ".class");
                                            return !isExcludedPath(classPath);
                                        })
                                        .map(plugin -> {
                                            // 设置插件的类加载器
                                            // Set class loader for plugins
                                            plugin.setPluginClassLoader(classLoader);
                                            plugin.setKey(extractPluginName(pluginDir));
                                            log.debug("Loaded TAR plugin: {} (version: {})", plugin.getClass().getName(), version);
                                            return plugin;
                                        })
                                        .collect(Collectors.toList());
                            });

                            allPlugins.addAll(plugins);
                        }
                        catch (Exception e) {
                            log.error("Failed to load plugins from directory: {}", pluginDir, e);
                        }
                    });
        }

        return allPlugins;
    }

    /**
     * 从 URL 下载 tar 文件到临时文件
     * Download tar file from URL to temporary file
     *
     * @param url tar 文件的 URL
     * URL of the tar file
     * @return 临时文件路径
     * Path to temporary file
     */
    private Path downloadTarFile(String url)
            throws IOException
    {
        log.info("Downloading tar file from URL: {}", url);
        Path tempFile = Files.createTempFile(TEMP_DIR_PREFIX, ".tar");

        try {
            // 创建信任所有证书的 SSL 上下文
            // Create SSL context that trusts all certificates
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (cert, authType) -> true)
                    .build();

            // 创建允许所有主机名的验证器
            // Create verifier that allows all hostnames
            SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE
            );

            // 创建 HttpClient
            // Create HttpClient
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslFactory)
                    .build()) {
                // 创建 GET 请求
                // Create GET request
                HttpGet request = new HttpGet(url);
                request.setConfig(RequestConfig.custom()
                        .setConnectTimeout(30000)
                        .setSocketTimeout(30000)
                        .build());

                // 执行请求并下载文件
                // Execute request and download file
                try (CloseableHttpResponse response = httpClient.execute(request);
                        InputStream in = response.getEntity().getContent();
                        OutputStream out = Files.newOutputStream(tempFile)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != 200) {
                        throw new IOException("HTTP request failed with status: " + statusCode);
                    }

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }

            return tempFile;
        }
        catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            try {
                Files.deleteIfExists(tempFile);
            }
            catch (IOException deleteError) {
                log.warn("Failed to delete temporary file after download failure: {}", tempFile, deleteError);
            }
            throw new IOException("Failed to download file from URL: " + url, e);
        }
    }

    /**
     * 创建临时目录
     * Create temporary directory
     */
    private Path createTempDirectory()
            throws IOException
    {
        return Files.createTempDirectory(TEMP_DIR_PREFIX + UUID.randomUUID());
    }

    /**
     * 解压 tar 文件到指定目录
     * Extract tar file to specified directory
     *
     * @param tarFile tar 文件路径
     * Path to tar file
     * @param destDir 目标目录
     * Destination directory
     */
    private void extractTarFile(Path tarFile, Path destDir)
            throws IOException
    {
        log.info("Extracting tar file to: {}", destDir);

        try (InputStream fileIn = Files.newInputStream(tarFile);
                BufferedInputStream buffIn = new BufferedInputStream(fileIn);
                GZIPInputStream gzipIn = new GZIPInputStream(buffIn);
                TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                Path outPath = destDir.resolve(entry.getName());
                Files.createDirectories(outPath.getParent());

                try (OutputStream out = Files.newOutputStream(outPath)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = tarIn.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    /**
     * 清理临时目录
     * Clean up temporary directory
     */
    private void cleanupTempDirectory(Path directory)
    {
        try {
            FileUtils.deleteDirectory(directory.toFile());
        }
        catch (IOException e) {
            log.warn("Failed to cleanup temporary directory: {}", directory, e);
        }
    }

    /**
     * 检查是否为合法 Tar 文件路径
     * Check if it's a valid Tar file path
     *
     * @param path file path
     * @return true if it's a valid Tar file
     */
    private boolean isValidTarPath(Path path)
    {
        String pathStr = path.toString();
        return (pathStr.startsWith("http") || pathStr.startsWith("https"))
                && (pathStr.endsWith("tar.gz") || pathStr.endsWith("tar") || pathStr.endsWith("tgz"));
    }

    /**
     * 检查是否合法的 Tar 文件，通过解压文件头检查
     * Check if it's a valid Tar file, by checking the file header
     *
     * @param path file path
     * @return true if it's a valid Tar file
     */
    private boolean isValidTarFile(Path path)
    {
        try (InputStream fileIn = Files.newInputStream(path);
                BufferedInputStream buffIn = new BufferedInputStream(fileIn);
                GZIPInputStream gzipIn = new GZIPInputStream(buffIn)) {
            log.debug("Validating tar file: {}", path);
            log.debug("Tar file header: {}", gzipIn.readNBytes(512));
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }
}
