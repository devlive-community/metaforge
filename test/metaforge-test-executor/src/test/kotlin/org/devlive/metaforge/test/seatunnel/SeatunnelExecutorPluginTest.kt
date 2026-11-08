package org.devlive.metaforge.test.seatunnel

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.junit.Assert.assertNotNull
import org.junit.Assume
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SeatunnelExecutorPluginTest
{
    private val pluginName = "Seatunnel"

    /**
     * locateExecutorPluginsDir walks up from the working directory (the module base dir on
     * ci) until it finds the executor module: PluginPathUtils.findProjectRoot stops at the
     * first directory with a pom, which inside a surefire fork is the test module itself or
     * the installed artifact in the local repository, and neither contains the plugin.
     */
    private fun locateExecutorPluginsDir(): Path?
    {
        var dir: Path? = Paths.get("").toAbsolutePath()
        repeat(6) {
            val current = dir ?: return null
            val candidate = current.resolve("executor/metaforge-executor-seatunnel")
            if (Files.isDirectory(candidate)) {
                return candidate
            }
            dir = current.parent
        }
        return null
    }

    @Test
    fun test()
    {
        val pluginsDir = locateExecutorPluginsDir()
        // The change-scoped ci build compiles only what a push touches; the executor module
        // is absent there and the plugin cannot load from a directory never compiled.
        Assume.assumeTrue("the seatunnel executor module is not built",
            pluginsDir != null && Files.isDirectory(pluginsDir.resolve("target/classes")))
        val config = PluginConfigure.builder()
            .pluginsDir(pluginsDir)
            .build()

        val pluginManager = PluginManager(config).apply { start() }
        assertNotNull("the $pluginName plugin must be registered", pluginManager.getPlugin(pluginName).orElse(null))
    }
}
