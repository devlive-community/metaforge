package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class PluginConfigure
{
    private static final Set<String> LOADER_PACKAGES = Set.of(
            "java.",
            "javax.",
            "com.google.",
            "org.",
            "ch.qos.logback.",
            "org.slf4j."
    );

    @Setter
    private Path pluginsDir;
    private boolean autoReload;
    private long scanInterval;
    private String pluginConfigFile;

    // 扫描层级，只有目录情况下才会生效
    // Scan depth, only effective when scanning directory
    private int scanDepth;

    // 0 根据 CPU 自动选择（最多 4），1 恢复串行加载
    // 0 selects up to 4 workers based on available CPUs; 1 loads sequentially
    private int loadThreads;

    // 自动清理, 只有卸载时才会生效
    // Auto cleanup, only effective when unloading
    private boolean autoCleanup;

    // 同一目录下多个插件是否共享类加载器
    // Whether multiple plugins in the same directory share the class loader
    private boolean shareClassLoaderWhenSameDir;

    // 优先使用父类加载器包列表
    // List of parent class loader packages to be prioritized
    @Builder.Default
    private Set<String> parentClassLoaderPackages = new HashSet<>(LOADER_PACKAGES);

    /**
     * 添加父类加载器包
     * Add parent class loader packages
     *
     * @param packageNames 父类加载器包名称 Parent class loader package name
     */
    public void addParentClassLoaderPackage(Set<String> packageNames)
    {
        this.parentClassLoaderPackages.addAll(packageNames);
    }

    public static PluginConfigure defaultConfig()
    {
        return PluginConfigure.builder()
                .pluginsDir(Paths.get("plugins"))
                .autoReload(false)
                .scanInterval(5000)
                .pluginConfigFile("plugin.properties")
                .scanDepth(1)
                .autoCleanup(true)
                .build();
    }
}
