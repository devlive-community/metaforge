package org.devlive.metaforge.spi

import com.google.common.collect.Lists
import org.devlive.metaforge.spi.connection.JdbcConfigure
import org.devlive.metaforge.spi.model.Response
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger
import org.testcontainers.containers.Network
import org.testcontainers.lifecycle.Startables
import org.testcontainers.shaded.org.awaitility.Awaitility.given
import java.util.*
import java.util.concurrent.TimeUnit

class PluginTest {
    private val log = getLogger(this.javaClass)
    private val plugin = MySQLPlugin()

    private val host = this.javaClass.simpleName
    private var container: MySQLContainer? = null
    private var configure: JdbcConfigure? = null

    @Before
    fun before() {
        val network = Network.newNetwork()
        container = MySQLContainer()
                .withNetwork(network)
                .withNetworkAliases(host)
        container?.portBindings = Lists.newArrayList(String.format("%s:%s", MySQLContainer.PORT, 3306))

        Startables.deepStart(java.util.stream.Stream.of(container)).join()

        log.info("Waiting for the container to be ready...")
        given().ignoreExceptions()
                .await()
                .atMost(10000, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
        log.info("Container started")

        configure = JdbcConfigure()
        configure!!.host = container?.host ?: host
        configure!!.port = MySQLContainer.PORT
        configure!!.username = Optional.of("root")
        configure!!.password = Optional.of(MySQLContainer.PASSWORD)
        configure!!.ssl = Optional.of(true)
        configure!!.jdbcDriver = "com.mysql.cj.jdbc.Driver"
        configure!!.jdbcType = "mysql"
    }

    @Test
    fun testValidator() {
        log.info(plugin.validator())
        Assert.assertTrue(plugin.validator().equals("SELECT version() AS version"))
    }

    @Test
    fun testExecute() {
        plugin.connect(configure)
        val response: Response = plugin.execute(plugin.validator())
        log.info("================ plugin executed information =================")
        if (!response.isSuccessful) {
            log.error("Message: {}", response.message)
        } else {
            response.columns.forEach { column -> log.info(column.toString()) }
        }
        Assert.assertTrue(response.isSuccessful)
    }
}
