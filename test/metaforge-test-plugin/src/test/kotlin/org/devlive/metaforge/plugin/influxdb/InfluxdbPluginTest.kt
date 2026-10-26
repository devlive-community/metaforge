package org.devlive.metaforge.plugin.influxdb

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Assert.assertNotNull
import org.junit.Test

class InfluxdbPluginTest
{
    private val pluginManager: PluginManager
    private val pluginName = "Influxdb"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("plugin/metaforge-plugin-influxdb"))
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        assertNotNull(pluginManager.getPlugin(pluginName).get())
    }
}
