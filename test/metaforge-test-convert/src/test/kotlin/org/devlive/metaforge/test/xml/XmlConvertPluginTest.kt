package org.devlive.metaforge.test.xml

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import lombok.extern.slf4j.Slf4j
import org.junit.Test

@Slf4j
class XmlConvertPluginTest
{
    private val pluginManager: PluginManager
    private val pluginName: String = "XmlConvert"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("convert/metaforge-convert-xml"))
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
