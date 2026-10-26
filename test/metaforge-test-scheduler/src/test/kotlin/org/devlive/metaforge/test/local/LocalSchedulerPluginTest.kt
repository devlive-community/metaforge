package org.devlive.metaforge.test.local

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalSchedulerModuleTest
{
    private val pluginManager: PluginManager
    private val pluginName = "LocalScheduler"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("scheduler/metaforge-scheduler-local"))
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        val plugin = pluginManager.getPlugin(pluginName)
        assertNotNull(plugin.get())
    }
}
