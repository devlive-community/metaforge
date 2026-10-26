package org.devlive.metaforge.plugin

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Configure
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.Network
import org.testcontainers.lifecycle.Startables
import org.testcontainers.shaded.org.awaitility.Awaitility.given
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_INFERRED", "SA_LOCAL_SELF_COMPARISON", "EI_EXPOSE_REP2", "NP_NONNULL_RETURN_VIOLATION"])
abstract class BaseServiceTest<T : BaseContainer<T>>(
    private val pluginName: String,
    protected val container: T,
    private val configFile: String
)
{
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    protected lateinit var pluginManager: PluginManager
    protected lateinit var plugin: Plugin

    @Before
    fun setUp()
    {
        initContainer()
        initPluginManager()
    }

    private fun initContainer()
    {
        val network = Network.newNetwork()

        container.withNetwork(network)
            .withNetworkAliases(this.javaClass.simpleName)

        // Start container
        Startables.deepStart(Stream.of(container)).join()

        // Wait for container to be ready
        given().ignoreExceptions()
            .await()
            .atMost(1, TimeUnit.MINUTES)
        log.info("Container started: {}", container.javaClass.simpleName)

        // Log all mapped ports for debugging
        container.getAllPorts().forEach { (name, port) ->
            log.info("Port mapping for {}: {}", name, port)
        }
    }

    private fun initPluginManager()
    {
        val configFile = this::class.java.classLoader.getResource(configFile)
        requireNotNull(configFile) { "Config file not found: $configFile" }
        log.info("Specified config file: {}", configFile)

        val projectRoot = PluginPathUtils.findProjectRoot()
        log.info("Project root: {}", projectRoot)
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve(Path.of(configFile.path)))
            .build()

        log.info("Initializing plugin manager")
        pluginManager = PluginManager(config).apply { start() }
        plugin = pluginManager.getPlugin(pluginName).get()
        assertNotNull("Plugin not found: $pluginName", plugin)
    }

    @Test
    open fun testService()
    {
        val configure = createConfigure()
        val pluginService = plugin.getService(PluginService::class.java)
        val response = pluginService.execute(configure, pluginService.validator())
        assertNotNull(response)
    }

    protected abstract fun createConfigure(): Configure
}
