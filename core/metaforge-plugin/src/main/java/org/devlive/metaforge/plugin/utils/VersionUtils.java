package org.devlive.metaforge.plugin.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Version utility for plugin system
 */
@Slf4j
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "SR_NOT_CHECKED"})
public class VersionUtils
{
    private VersionUtils() {}

    private static final String VERSION_PROPERTY_FILE = "plugin.properties";
    private static final String VERSION_PROPERTY_KEY = "plugin.version";
    private static final String DEFAULT_VERSION = "1.0.0";

    /**
     * Determine plugin version from various sources
     *
     * @param pluginPath plugin directory path
     * @return version string
     */
    public static String determinePluginVersion(Path pluginPath)
    {
        // 1. Try to read from plugin.properties
        String version = readVersionFromProperties(pluginPath);
        if (version != null) {
            return version;
        }

        // 2. Try to read from MANIFEST.MF
        version = readVersionFromManifest(pluginPath);
        if (version != null) {
            return version;
        }

        // 3. Try to read from pom.properties
        version = readVersionFromPomProperties(pluginPath);
        if (version != null) {
            return version;
        }

        // 4. If JAR file exists, try to read from it
        version = findVersionInJars(pluginPath);
        if (version != null) {
            return version;
        }

        // 5. If all attempts fail, return default version
        log.warn("Could not determine plugin version for {}, using default version: {}",
                pluginPath, DEFAULT_VERSION);
        return DEFAULT_VERSION;
    }

    /**
     * Read version from plugin.properties file
     */
    private static String readVersionFromProperties(Path pluginPath)
    {
        Path propertiesPath = pluginPath.resolve(VERSION_PROPERTY_FILE);
        if (Files.exists(propertiesPath)) {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(propertiesPath)) {
                props.load(is);
                String version = props.getProperty(VERSION_PROPERTY_KEY);
                if (isValidVersion(version)) {
                    log.debug("Found version {} in {}", version, VERSION_PROPERTY_FILE);
                    return version;
                }
            }
            catch (IOException e) {
                log.debug("Failed to read version from {}", VERSION_PROPERTY_FILE, e);
            }
        }
        return null;
    }

    /**
     * Read version from MANIFEST.MF file
     */
    private static String readVersionFromManifest(Path pluginPath)
    {
        Path manifestPath = pluginPath.resolve("META-INF/MANIFEST.MF");
        if (Files.exists(manifestPath)) {
            try (InputStream is = Files.newInputStream(manifestPath)) {
                Manifest manifest = new Manifest(is);
                String version = getVersionFromManifest(manifest);
                if (version != null) {
                    log.debug("Found version {} in MANIFEST.MF", version);
                    return version;
                }
            }
            catch (IOException e) {
                log.debug("Failed to read version from MANIFEST.MF", e);
            }
        }
        return null;
    }

    /**
     * Extract version from Manifest attributes
     */
    private static String getVersionFromManifest(Manifest manifest)
    {
        Attributes attributes = manifest.getMainAttributes();

        // Try different possible version attributes
        String[] versionAttributes = {
                "Implementation-Version",
                "Specification-Version",
                "Bundle-Version",
                "Project-Version",
                "Version"
        };

        for (String attr : versionAttributes) {
            String version = attributes.getValue(attr);
            if (isValidVersion(version)) {
                log.debug("Found version {} using attribute {}", version, attr);
                return version;
            }
        }
        return null;
    }

    /**
     * Read version from Maven pom.properties
     */
    private static String readVersionFromPomProperties(Path pluginPath)
    {
        try {
            // Look for pom.properties in META-INF/maven directory
            Path mavenDir = pluginPath.resolve("META-INF/maven");
            if (Files.exists(mavenDir)) {
                try (Stream<Path> pathStream = Files.walk(mavenDir)) {
                    List<Path> pomProperties = pathStream
                            .filter(p -> p.getFileName().toString().equals("pom.properties"))
                            .collect(Collectors.toList());

                    for (Path propFile : pomProperties) {
                        Properties props = new Properties();
                        try (InputStream is = Files.newInputStream(propFile)) {
                            props.load(is);
                            String version = props.getProperty("version");
                            if (isValidVersion(version)) {
                                log.debug("Found version {} in pom.properties", version);
                                return version;
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            log.debug("Failed to read version from pom.properties", e);
        }
        return null;
    }

    /**
     * Find version in any JAR files in the plugin directory
     */
    private static String findVersionInJars(Path pluginPath)
    {
        try {
            if (Files.isDirectory(pluginPath)) {
                try (Stream<Path> pathStream = Files.walk(pluginPath)) {
                    List<Path> jarFiles = pathStream
                            .filter(p -> p.toString().endsWith(".jar"))
                            .collect(Collectors.toList());
                    for (Path jarPath : jarFiles) {
                        String version = readVersionFromPluginJar(jarPath);
                        if (version != null) {
                            return version;
                        }
                    }
                }
            }
            else if (pluginPath.toString().endsWith(".jar")) {
                return readVersionFromPluginJar(pluginPath);
            }
        }
        catch (IOException e) {
            log.debug("Failed to search for JAR files in {}", pluginPath, e);
        }
        return null;
    }

    /**
     * Read version from a JAR file, but only if it contains a Plugin class
     */
    private static String readVersionFromPluginJar(Path jarPath)
    {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // 首先检查是否包含 Plugin 类的实现
            // Check if the JAR contains a Plugin class
            if (!containsPluginClass(jarFile)) {
                return null;
            }

            // TODO: 如果是插件JAR，则从文件名中提取版本
            // TODO: If the JAR is a plugin JAR, extract the version from the file name
            String fileName = jarPath.getFileName().toString();

            // 如果文件名中没有版本，尝试从 MANIFEST.MF 读取
            // If the version is not in the file name, try to read it from the MANIFEST.MF
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                String version = getVersionFromManifest(manifest);
                if (version != null) {
                    log.debug("Found version {} from plugin JAR manifest: {}", version, fileName);
                    return version;
                }
            }
        }
        catch (IOException e) {
            log.debug("Failed to read from JAR: {}", jarPath, e);
        }
        return null;
    }

    /**
     * Check if JAR contains a Plugin class implementation
     */
    private static boolean containsPluginClass(JarFile jarFile)
    {
        try {
            log.debug("Checking JAR: {}", jarFile.getName());

            // 读取所有类文件记录
            // Read all class files
            List<JarEntry> classEntries = jarFile.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"))
                    .filter(entry -> !entry.getName().endsWith("module-info.class"))
                    .collect(Collectors.toList());

            for (JarEntry entry : classEntries) {
                try (DataInputStream dis = new DataInputStream(jarFile.getInputStream(entry))) {
                    // 检查class文件魔数
                    // Check class file magic number
                    if (dis.readInt() != 0xCAFEBABE) {
                        continue;
                    }

                    // 跳过版本信息
                    // Skip version information
                    dis.skipBytes(4);

                    // 读取常量池大小
                    // Read constant pool size
                    int constantPoolCount = dis.readUnsignedShort();
                    // 跳过常量池
                    // Skip constant pool
                    for (int i = 1; i < constantPoolCount; i++) {
                        int tag = dis.readUnsignedByte();
                        switch (tag) {
                            case 7: // Class
                            case 8: // String
                                dis.skipBytes(2);
                                break;
                            case 9: // Fieldref
                            case 10: // Methodref
                            case 11: // InterfaceMethodref
                            case 12: // NameAndType
                                dis.skipBytes(4);
                                break;
                            case 1: // Utf8
                                int length = dis.readUnsignedShort();
                                byte[] bytes = new byte[length];
                                dis.readFully(bytes);
                                String value = new String(bytes, StandardCharsets.UTF_8);
                                if ("org/devlive/metaforge/plugin/Plugin".equals(value)) {
                                    log.debug("Found Plugin reference in {}", entry.getName());
                                    return true;
                                }
                                break;
                            case 3: // Integer
                            case 4: // Float
                                dis.skipBytes(4);
                                break;
                            case 5: // Long
                            case 6: // Double
                                dis.skipBytes(8);
                                i++; // Long 和 Double 占用两个常量池项 / Long and Double take up two constant pool items
                                break;
                            case 15: // MethodHandle
                                dis.skipBytes(3);
                                break;
                            case 16: // MethodType
                                dis.skipBytes(2);
                                break;
                            case 17: // Dynamic
                            case 18: // InvokeDynamic
                                dis.skipBytes(4);
                                break;
                            default:
                                log.debug("Unknown constant pool tag {} in {}", tag, entry.getName());
                                break;
                        }
                    }
                }
                catch (Exception e) {
                    log.debug("Failed to process class {}: {}", entry.getName(), e.getMessage());
                }
            }
            return false;
        }
        catch (Exception e) {
            log.error("Failed to check JAR {}: {}", jarFile.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Read version from a JAR file
     */
    private static String readVersionFromJar(Path jarPath)
    {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                String version = getVersionFromManifest(manifest);
                if (version != null) {
                    log.debug("Found version {} in JAR: {}", version, jarPath);
                    return version;
                }
            }
        }
        catch (IOException e) {
            log.debug("Failed to read version from JAR: {}", jarPath, e);
        }
        return null;
    }

    /**
     * Validate version format
     */
    private static boolean isValidVersion(String version)
    {
        if (version == null || version.isEmpty()) {
            return false;
        }
        // Validate version format: major.minor.patch(-qualifier)?
        return version.matches("^\\d+(\\.\\d+)*(-[a-zA-Z0-9]+)?$");
    }

    /**
     * Compare two version strings
     *
     * @return negative if version1 < version2, 0 if equal, positive if version1 > version2
     */
    public static int compareVersions(String version1, String version2)
    {
        if (version1 == null) {
            version1 = DEFAULT_VERSION;
        }
        if (version2 == null) {
            version2 = DEFAULT_VERSION;
        }

        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int length = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < length; i++) {
            int v1Part = i < v1Parts.length ?
                    Integer.parseInt(v1Parts[i].replaceAll("[^0-9].*$", "")) : 0;
            int v2Part = i < v2Parts.length ?
                    Integer.parseInt(v2Parts[i].replaceAll("[^0-9].*$", "")) : 0;

            if (v1Part != v2Part) {
                return v1Part - v2Part;
            }
        }
        return 0;
    }

    /**
     * Check if version meets minimum required version
     */
    public static boolean isVersionCompatible(String version, String minimumVersion)
    {
        return compareVersions(version, minimumVersion) >= 0;
    }
}
