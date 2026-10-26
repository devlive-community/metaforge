package org.devlive.metaforge.test.spi

import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Assert.assertNotNull
import org.junit.Test

class NoneExecutorServiceTest
{
    private val pluginManager: PluginManager
    private val pluginName: String = "NoneExecutor"

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
        pluginManager.getPlugin(pluginName).ifPresent { plugin ->
            val service = plugin.getService(NoneExecutorService::class.java)
            service.start(ExecutorRequest(null, ExecutorConfigure(null), ExecutorConfigure(null)))
            assertNotNull(service)
        }
    }
}
