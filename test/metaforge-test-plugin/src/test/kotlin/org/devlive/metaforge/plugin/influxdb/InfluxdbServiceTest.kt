package org.devlive.metaforge.plugin.influxdb

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.junit.Assert.assertNotNull
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.Network
import org.testcontainers.lifecycle.Startables
import org.testcontainers.shaded.org.awaitility.Awaitility.given
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_INFERRED", "SA_LOCAL_SELF_ASSIGNMENT"])
class InfluxdbServiceTest
{
    companion object
    {
        @BeforeClass
        @JvmStatic
        fun assumeDockerAvailable()
        {
            // Docker Desktop（Engine 29）会以 HTTP 400 拒绝 docker-java 默认 API 版本，钉一个双方都支持的版本
            if (System.getProperty("api.version") == null && System.getenv("DOCKER_API_VERSION") == null)
            {
                System.setProperty("api.version", "1.41")
            }
            // 无 Docker 环境时跳过容器集成测试，CI（带 Docker）仍然执行
            Assume.assumeTrue(
                "Docker is not available, skipping Influxdb container tests",
                DockerClientFactory.instance().isDockerAvailable
            )
        }
    }

    private val log = getLogger(this.javaClass)
    private val pluginManager: PluginManager
    private var container: InfluxdbContainer
    private val pluginName = "Influxdb"

    init
    {
        val network = Network.newNetwork()

        container = InfluxdbContainer()
            .withNetwork(network)
            .withNetworkAliases(this.javaClass.simpleName)

        container.portBindings = listOf(String.format("%s:%s", InfluxdbContainer.PORT, InfluxdbContainer.DOCKER_PORT))

        Startables.deepStart(java.util.stream.Stream.of(container))
            .join()

        given().ignoreExceptions()
            .await()
            .atMost(1, TimeUnit.MINUTES)
        log.info("Influxdb container started")

        val configFile = this::class
            .java
            .classLoader
            .getResource("influxdb-config.properties")
        assertNotNull(configFile)
        log.info("Specified config file: {}", configFile)

        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve(Path.of(configFile.path)))
            .build()

        log.info("Initializing plugin manager")
        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        val plugin = pluginManager.getPlugin(pluginName).get()
        assertNotNull(plugin)

        val configure: RuntimeConfigure = RuntimeConfigure.builder()
            .host(container.host)
            .port(InfluxdbContainer.PORT)
            .username(Optional.of(InfluxdbContainer.USERNAME))
            .password(Optional.of(InfluxdbContainer.PASSWORD))
            .database(Optional.of(InfluxdbContainer.BUCKET))
            .plugin(plugin)
            .pluginManager(pluginManager)
            .env(
                Optional.of(
                    mapOf(
                        "useEncryption" to false,
                        "useHTTP2" to false,
                        "org" to "TestOrg"
                    )
                )
            )
            .build()

        val pluginService = plugin.getService(PluginService::class.java)
        val response = pluginService.execute(configure, pluginService.validator())
        assertNotNull(response)
    }
}
