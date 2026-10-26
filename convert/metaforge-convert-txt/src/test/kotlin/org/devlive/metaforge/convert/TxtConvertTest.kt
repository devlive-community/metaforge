package org.devlive.metaforge.convert

import com.google.inject.Guice
import com.google.inject.Injector
import org.devlive.metaforge.convert.ConvertFilter
import org.devlive.metaforge.convert.ConvertManager
import org.devlive.metaforge.convert.model.ConvertRequest
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TxtConvertTest
{
    private val log = getLogger(TxtConvertTest::class.java)
    private val name = "Txt"
    private var injector: Injector? = null
    private val request: ConvertRequest = ConvertRequest()

    @Before
    fun before()
    {
        injector = Guice.createInjector(ConvertManager())

        request.name = "test"
        request.path = System.getProperty("user.dir")
        request.headers = listOf("name", "age")

        val l1 = listOf("Test", 12)
        val l2 = listOf("Test1", 121)
        request.columns = listOf(l1, l2)
    }

    @Test
    fun testFormat()
    {
        injector?.let { injector ->
            ConvertFilter.filter(injector, name)
                .ifPresent { file ->
                    request.delimiter = "[&&&]"
                    val response = file.format(request)
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
    }

    @Test
    fun testFormatStream()
    {
        injector?.let { injector ->
            ConvertFilter.filter(injector, name)
                .ifPresent { file ->
                    request.delimiter = "[&&&]"
                    request.stream = FileInputStream(File("${System.getProperty("user.dir")}/${request.name}.txt"))
                    val response = file.formatStream(request)
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
    }

    @Test
    fun testWriter()
    {
        injector?.let { injector ->
            ConvertFilter.filter(injector, name)
                .ifPresent { file ->
                    assertFalse {
                        file.writer(request)
                            .successful == true
                    }

                    request.delimiter = "[&&&]"
                    assertTrue {
                        file.writer(request)
                            .successful == true
                    }
                }
        }
    }

    @Test
    fun testReader()
    {
        injector?.let { injector ->
            ConvertFilter.filter(injector, name)
                .ifPresent { file ->
                    assertFalse {
                        file.reader(request)
                            .successful == true
                    }

                    request.delimiter = "[&&&]"
                    val response = file.reader(request)
                    log.info("headers: ${response.headers}")
                    response.columns
                        .let { columns ->
                            columns.forEach {
                                log.info("columns: $it")
                            }
                        }
                    assertTrue {
                        response.successful == true
                    }
                }
        }
    }
}
