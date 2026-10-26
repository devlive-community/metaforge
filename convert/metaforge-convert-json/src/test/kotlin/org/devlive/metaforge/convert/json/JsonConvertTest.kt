package org.devlive.metaforge.convert.json

import org.devlive.metaforge.convert.model.ConvertRequest
import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertTrue


class JsonConvertTest
{
    private val log = getLogger(this::class.java)
    private val name = "JsonConvert"
    private val pluginManager: PluginManager
    private val request: ConvertRequest = ConvertRequest()

    init
    {
        val projectRoot: Path = PluginPathUtils.findProjectRoot()
        val config: PluginConfigure? = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("convert/metaforge-convert-json"))
            .scanDepth(2)
            .build()

        pluginManager = PluginManager(config)
        pluginManager.start()

        request.name = "test"
        request.path = System.getProperty("user.dir")
        request.headers = listOf("name", "age", "datatime")

        val timeString = LocalDateTime.parse("2023-12-01T10:30:00").format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val l1 = listOf("Test", 12, timeString)
        val l2 = listOf("Test1", 121, timeString)
        request.columns = listOf(l1, l2)
        request.columns = listOf(l1, l2)
    }

    @Test
    fun testFormat()
    {
        pluginManager.getPlugin(name)
            .ifPresent { plugin ->
                val service = plugin.getService(JsonConvertService::class.java)
                assertNotNull(service)

                val response = service.format(request)
                log.info("headers: [ ${response.headers} ]")
                response.columns
                    .let { columns ->
                        columns.forEachIndexed { index, line ->
                            log.info("index: [ $index ], line: [ $line ]")
                        }
                    }

                assertTrue {
                    response.successful == true
                }
            }
    }

    @Test
    fun testFormatStream()
    {
        pluginManager.getPlugin(name)
            .ifPresent { plugin ->
                val service = plugin.getService(JsonConvertService::class.java)
                assertNotNull(service)

                request.stream = FileInputStream(File("${System.getProperty("user.dir")}/${request.name}.json"))
                val response = service.formatStream(request)
                log.info("headers: [ ${response.headers} ]")
                response.columns
                    .let { columns ->
                        columns.forEachIndexed { index, line ->
                            log.info("index: [ $index ], line: [ $line ]")
                        }
                    }
                assertTrue {
                    response.successful == true
                }
            }
    }

    @Test
    fun testWriter()
    {
        pluginManager.getPlugin(name)
            .ifPresent { plugin ->
                val service = plugin.getService(JsonConvertService::class.java)
                assertNotNull(service)

                assertTrue {
                    service.writer(request).successful == true
                }
            }
    }

    @Test
    fun testReader()
    {
        pluginManager.getPlugin(name)
            .ifPresent { plugin ->
                val service = plugin.getService(JsonConvertService::class.java)
                assertNotNull(service)

                service.writer(request)

                val response = service.reader(request)
                log.info("headers: ${response.headers}")
                response.columns
                    .forEach {
                        log.info("columns: $it")
                    }
                assertTrue {
                    response.successful == true
                }
            }
    }
}
