package org.devlive.metaforge.plugin.dolphindb

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Configure
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.io.path.Path

class DolphinDBServiceTest
{
    companion object
    {
        private val log = LoggerFactory.getLogger(DolphinDBServiceTest::class.java)
        private val CONTAINER = DolphinDBContainer()
        private const val PLUGIN_NAME = "DolphinDB"
    }

    private lateinit var pluginManager: PluginManager
    private lateinit var plugin: Plugin
    private lateinit var configure: Configure

    @Before
    fun init()
    {
        initPluginManager()
    }

    @After
    fun destroy()
    {
        if (::pluginManager.isInitialized)
        {
            pluginManager.destroy()
        }
        CONTAINER.stop()
    }

    private fun initPluginManager()
    {
        CONTAINER.start()

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

        plugin = pluginManager.getPlugin(PLUGIN_NAME)
            .orElseThrow { IllegalStateException("Plugin not found: $PLUGIN_NAME") }

        initConfigure()
    }

    private fun initConfigure()
    {
        if (! CONTAINER.isRunning)
        {
            throw RuntimeException("Container is stopped.")
        }

        configure = Configure.builder()
            .port(CONTAINER.port)
            .host(CONTAINER.host)
            .plugin(plugin)
            .pluginManager(pluginManager)
            .build()

        log.info("Configure initialized with host: {}:{}", CONTAINER.host, CONTAINER.port)
    }

    @Test
    fun validator()
    {
        val service = plugin.getService(PluginService::class.java)
        val result = service.execute(configure, service.validator())
        Assert.assertTrue("Validator should succeed", result.isSuccessful)
        log.info("Validator test passed successfully")
    }

    @Test
    fun testConnection()
    {
        val service = plugin.getService(PluginService::class.java)
        val result = service.execute(configure, service.validator())

        Assert.assertTrue("Connection should be successful", result.isSuccessful)
        Assert.assertNotNull("Result should not be null", result)

        log.info("Connection test completed: {}", result)
    }

    @Test
    fun testContainerHealth()
    {
        Assert.assertTrue("Container should be running", CONTAINER.isRunning)
        Assert.assertNotNull("Container host should not be null", CONTAINER.host)
        Assert.assertTrue("Container port should be valid", CONTAINER.port > 0)

        log.info(
            "Container health check passed - Host: {}, Port: {}",
            CONTAINER.host, CONTAINER.port
        )
    }

    @Test
    fun testPluginLoading()
    {
        Assert.assertNotNull("Plugin should be loaded", plugin)
        Assert.assertEquals("Plugin name should match", PLUGIN_NAME, plugin.name)

        val service = plugin.getService(PluginService::class.java)
        Assert.assertNotNull("Plugin service should be available", service)

        log.info("Plugin loading test passed - Plugin: {}", plugin.name)
    }

    @Test
    fun testConfigureValidation()
    {
        Assert.assertNotNull("Configure should not be null", configure)
        Assert.assertNotNull("Host should not be null", configure.host)
        Assert.assertTrue("Port should be valid", configure.port > 0)

        log.info("Configure validation passed - {}:{}", configure.host, configure.port)
    }
}
