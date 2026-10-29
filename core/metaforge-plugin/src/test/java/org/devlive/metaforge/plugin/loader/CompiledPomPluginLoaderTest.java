package org.devlive.metaforge.plugin.loader;

import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginConfigure;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.plugin.PluginType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.ToolProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class CompiledPomPluginLoaderTest
{
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private Path module(String name, String type)
            throws Exception
    {
        Path root = temporary.newFolder(name).toPath();
        Path classes = root.resolve("target/classes");
        Files.createDirectories(classes);
        Path source = root.resolve(name + ".java");
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Files.writeString(source, "package fixtures; public class " + name + " extends org.devlive.metaforge.plugin.Plugin {"
                + "public org.devlive.metaforge.plugin.PluginType getType() { return org.devlive.metaforge.plugin.PluginType." + type + "; }}");
        Assert.assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-classpath", System.getProperty("java.class.path"), "-d", classes.toString(), source.toString()));
        return root;
    }

    @Test
    public void compiledPluginsKeepTheirActualClassLoader()
            throws Exception
    {
        Path root = module("ProbeExecutorPlugin", "EXECUTOR");
        List<Plugin> loaded = new CompiledPomPluginLoader().load(root, Set.of("java.", "org.devlive.metaforge.plugin."));
        Assert.assertEquals(1, loaded.size());
        Plugin plugin = loaded.get(0);
        Assert.assertNotNull(plugin.getPluginClassLoader());
        Assert.assertSame(plugin.getClass(), plugin.getPluginClassLoader().loadClass("fixtures.ProbeExecutorPlugin"));
        Assert.assertNotNull(plugin.getPluginClassLoader().getPluginVersion());
        plugin.getPluginClassLoader().close();
    }

    @Test
    public void aManifestRegistersAllCompiledExecutorsAndSchedulers()
            throws Exception
    {
        Path executor = module("FirstExecutorPlugin", "EXECUTOR");
        Path scheduler = module("SecondSchedulerPlugin", "SCHEDULER");
        Path manifest = temporary.getRoot().toPath().resolve("plugins.properties");
        Files.writeString(manifest, "executor=" + executor.resolve("pom.xml") + "\nscheduler=" + scheduler.resolve("pom.xml") + "\n");
        PluginManager manager = new PluginManager(PluginConfigure.builder().pluginsDir(manifest).autoCleanup(false).build());
        try {
            manager.start();
            Assert.assertEquals(2, manager.getPluginInfos().size());
            Assert.assertTrue(manager.getPluginInfos().stream().anyMatch(plugin -> plugin.getType() == PluginType.EXECUTOR));
            Assert.assertTrue(manager.getPluginInfos().stream().anyMatch(plugin -> plugin.getType() == PluginType.SCHEDULER));
        }
        finally {
            manager.stop();
        }
    }
}
