package org.devlive.metaforge.plugin.dolphindb

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.io.path.Path

class DolphinDBPluginTest
{
    private val log = LoggerFactory.getLogger(DolphinDBPluginTest::class.java)
    private lateinit var pluginManager: PluginManager

    @Before
    fun init()
    {
        initPluginManager()
    }

    private fun initPluginManager()
    {
        val configFile = this::class.java.classLoader.getResource("config.properties")
        log.info("Specified config file: {}", configFile)

        val projectRoot = PluginPathUtils.findProjectRoot()
        log.info("Project root: {}", projectRoot)

        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve(Path(configFile?.path ?: "")))
            .build()

        log.info("Initializing plugin manager")
        pluginManager = PluginManager(config)
        pluginManager.start()

        pluginManager.pluginInfos.forEach {
            log.info("Plugin: {}", it)
        }
    }

    @Test
    fun test()
    {
        Assert.assertNotNull(pluginManager.getPlugin("DolphinDB"))
    }

    @Test
    fun testPluginExists()
    {
        val plugin = pluginManager.getPlugin("DolphinDB")
        Assert.assertNotNull("DolphinDB plugin should exist", plugin)
        log.info("DolphinDB plugin loaded successfully: {}", plugin)
    }
}
