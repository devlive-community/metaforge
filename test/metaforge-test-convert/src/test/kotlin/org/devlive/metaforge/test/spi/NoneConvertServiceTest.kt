package org.devlive.metaforge.test.spi

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Test
import kotlin.test.assertNotNull

class NoneConvertServiceTest
{
    private val pluginManager: PluginManager
    private val pluginName: String = "NoneConvert"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("test/metaforge-test-convert"))
            .scanDepth(2)
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        pluginManager.getPlugin(pluginName).ifPresent { plugin ->
            val service = plugin.getService(NoneConvertService::class.java)
            assertNotNull(service)
        }
    }
}
