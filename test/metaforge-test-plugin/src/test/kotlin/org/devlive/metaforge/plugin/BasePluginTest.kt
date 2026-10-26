package org.devlive.metaforge.plugin

import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Assert.assertNotNull
import org.junit.Test

abstract class BasePluginTest(
    private val pluginName: String,
    pluginHome: String
)
{
    private val pluginManager: PluginManager

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve(pluginHome))
            .build()
        config.addParentClassLoaderPackage(setOf("kotlin"))

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        val plugin = pluginManager.getPlugin(pluginName)
        assertNotNull(plugin.get())
    }
}
