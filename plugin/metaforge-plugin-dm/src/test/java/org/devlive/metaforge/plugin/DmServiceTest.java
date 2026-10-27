package org.devlive.metaforge.plugin;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.container.DmContainer;
import org.devlive.metaforge.plugin.utils.PluginPathUtils;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

import static org.devlive.metaforge.plugin.container.DMImages.DAMENG_8;

@Slf4j
public class DmServiceTest
{
    private static final DmContainer CONTAINER = new DmContainer(DAMENG_8)
            .withXa()
            .withLockWaitTimeout(50_000L);
    private static final String pluginName = "Dm";

    private PluginManager pluginManager;
    private Plugin plugin;
    private RuntimeConfigure configure;

    @BeforeClass
    public static void assumeDockerAvailable()
    {
        // 本地无 Docker 环境时跳过达梦容器集成测试，CI（带 Docker）仍然执行
        // Docker Desktop（Engine 29）会以 HTTP 400 拒绝 docker-java 默认 API 版本，钉一个双方都支持的版本
        if (System.getProperty("api.version") == null && System.getenv("DOCKER_API_VERSION") == null) {
            System.setProperty("api.version", "1.41");
        }
        boolean available;
        try {
            available = org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        }
        catch (Throwable ex) {
            available = false;
        }
        Assume.assumeTrue("Docker is not available, skipping Dameng container tests", available);
        // 镜像 hanyf/damengdb 仅发布 linux/amd64，Apple Silicon 等环境无法运行
        String arch = System.getProperty("os.arch", "");
        Assume.assumeTrue("Dameng image is amd64-only, skipping on " + arch,
                "amd64".equals(arch) || "x86_64".equals(arch));
    }

    @Before
    public void init()
    {
        this.initPluginManager();
    }

    @After
    public void destroy()
    {
        if (CONTAINER.isRunning()) {
            CONTAINER.stop();
        }
        if (pluginManager != null) {
            pluginManager.destroy();
        }
    }

    private void initPluginManager()
    {
        CONTAINER.start();

        URL configFile = this.getClass().getClassLoader().getResource("config.properties");
        log.info("Specified config file: {}", configFile);

        Path projectRoot = PluginPathUtils.findProjectRoot();
        log.info("Project root: {}", projectRoot);
        PluginConfigure config = PluginConfigure.builder()
                .pluginsDir(projectRoot.resolve(Path.of(configFile.getPath())))
                .build();

        log.info("Initializing plugin manager");
        pluginManager = new PluginManager(config);
        pluginManager.start();
        plugin = pluginManager.getPlugin(pluginName)
                .orElseThrow(() -> new IllegalStateException("Plugin not found: " + pluginName));

        initConfigure();
    }

    private void initConfigure()
    {
        if (!CONTAINER.isRunning()) {
            throw new RuntimeException("Container is stopped.");
        }

        configure = RuntimeConfigure.builder()
                .username(Optional.of(CONTAINER.getUsername()))
                .password(Optional.of(CONTAINER.getPassword()))
                .port(CONTAINER.getPort())
                .host(CONTAINER.getHost())
                .plugin(plugin)
                .pluginManager(pluginManager)
                .build();
    }

    @Test
    public void validator()
    {
        PluginService service = plugin.getService(PluginService.class);
        Assert.assertTrue(service.execute(configure, service.validator()).getIsSuccessful());
    }
}
