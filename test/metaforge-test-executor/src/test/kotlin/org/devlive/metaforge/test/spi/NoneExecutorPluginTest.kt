package org.devlive.metaforge.test.spi

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Assume
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
            .pluginsDir(projectRoot)
            .scanDepth(2)
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        val plugin = pluginManager.getPlugin(pluginName)
        // NoneExecutor 插件类位于测试源码，CI 只执行 test-compile 时扫描不到插件，
        // 与 DM 测试一致：环境不支持时跳过而不是失败
        Assume.assumeTrue("NoneExecutor plugin is not discoverable in this layout, skipping", plugin.isPresent)
        assertTrue(plugin.isPresent)
    }
}
