package org.devlive.metaforge.test.seatunnel.connector

import com.google.common.collect.Sets
import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.seatunnel.connector.ConnectorFactory
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger
import java.util.*

class ConnectorFactoryTest
{
    private val log = getLogger(this::class.java)
    private var input: ExecutorConfigure? = null

    @Before
    fun before()
    {
        val properties = Properties()
        properties["host"] = "127.0.0.1"
        properties["username"] = "root"
        properties["password"] = "123456"
        properties["database"] = "default"
        properties["sql"] = "SHOW DATABASES"
        input = ExecutorConfigure("ClickHouse", properties, Sets.newHashSet())
    }

    @Test
    fun createFormatter()
    {
        val factory = ConnectorFactory.createFormatter("Clickhouse", this.input)
        val map = factory.formatToMap()
        log.info("Format: {}", map)
        Assert.assertNotNull(factory.formatToMap())
    }
}
