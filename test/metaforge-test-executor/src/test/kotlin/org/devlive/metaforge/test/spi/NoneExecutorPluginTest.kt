package org.devlive.metaforge.test.spi

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Test
import kotlin.test.assertTrue

class NoneExecutorPluginTest
{
    private val pluginManager: PluginManager
    private val pluginName = "NoneExecutor"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("test/metaforge-test-executor"))
            .scanDepth(2)
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        assertTrue(pluginManager.getPlugin(pluginName).isPresent)
    }
}
