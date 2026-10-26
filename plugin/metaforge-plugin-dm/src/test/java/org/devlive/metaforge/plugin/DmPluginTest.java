package org.devlive.metaforge.plugin;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.utils.PluginPathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;

@Slf4j
public class DmPluginTest
{
    private PluginManager pluginManager;

    @Before
    public void init()
    {
        this.initPluginManager();
    }

    private void initPluginManager()
    {
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
    }

    @Test
    public void test()
    {
        Assert.assertNotNull(pluginManager.getPlugin("Dm"));
    }
}
