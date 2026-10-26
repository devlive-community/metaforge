package org.devlive.metaforge.test;

import org.devlive.metaforge.plugin.PluginConfigure;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.loader.InjectPluginLoader;
import org.devlive.metaforge.plugin.loader.PluginLoaderFactory;
import org.devlive.metaforge.plugin.utils.PluginPathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

public class LocalPluginTest
{
    private PluginManager pluginManager;

    @Before
    public void before()
    {
        Path projectRoot = PluginPathUtils.findProjectRoot();
        PluginConfigure config = PluginConfigure.builder()
                .pluginsDir(projectRoot.resolve("test/metaforge-test-plugin"))
                .scanDepth(2)
                .build();

        PluginLoaderFactory.registerLoader(new InjectPluginLoader());
        pluginManager = new PluginManager(config);
        pluginManager.start();
    }

    @Test
    public void testLoadPlugin()
    {
        Assert.assertFalse(pluginManager.getPlugin("Local").isEmpty());
    }

    @Test
    public void testAnnotationPlugin()
    {
        pluginManager.getPlugin("Annotation")
                .ifPresent(value -> {
                    LogService logService = value.getService(LogService.class);
                    logService.print();
                });
    }
}
