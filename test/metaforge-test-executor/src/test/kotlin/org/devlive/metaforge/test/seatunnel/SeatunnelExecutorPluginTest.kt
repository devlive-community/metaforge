package org.devlive.metaforge.test.seatunnel

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Assert.assertNotNull
import org.junit.Test

class SeatunnelExecutorPluginTest
{
    private val pluginManager: PluginManager
    private val pluginName = "Seatunnel"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("executor/metaforge-executor-seatunnel"))
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        assertNotNull(pluginManager.getPlugin(pluginName).get())
    }
}