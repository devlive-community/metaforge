package org.devlive.metaforge.test.lib.logger

import org.devlive.metaforge.lib.logger.logback.LogbackExecutor
import org.junit.Test
import org.slf4j.Logger
import kotlin.test.assertNotNull

class LogbackExecutorTest
{
    private val directory: String = System.getProperty("user.dir")
    private val name = javaClass.name + ".log"
    private val loggerExecutor: LogbackExecutor = LogbackExecutor(directory, name)
    private var logger: Logger = loggerExecutor.getLogger()

    @Test
    fun getLogger()
    {
        logger.info("info")
        logger.debug("debug")
        logger.error("error")
        logger.warn("warn")
        logger.trace("trace")
    }

    @Test
    fun destroy()
    {
        loggerExecutor.destroy()
        assertNotNull(logger)
    }
}
