package org.devlive.metaforge.convert.json

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import lombok.extern.slf4j.Slf4j
import org.junit.Test

@Slf4j
class XmlConvertPluginTest
{
    private val pluginManager: PluginManager
    private val pluginName: String = "JsonConvert"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("convert/metaforge-convert-json"))
            .scanDepth(2)
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        println(pluginManager.getPlugin(pluginName))
    }
}
