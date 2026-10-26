package org.devlive.metaforge.test

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.fs.FsRequest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.io.FileInputStream
import java.io.InputStream
import java.util.*

@SuppressFBWarnings(value = ["OBL_UNSATISFIED_OBLIGATION"])
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
abstract class BaseIOUtilsTest(
    private val pluginPrefix: String,
    private val ioUtils: Class<*>
)
{
    private val log: Logger = getLogger(this::class.java)
    private val request = FsRequest()
    private val fileName = "${pluginPrefix.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}FsPluginTest.kt"

    @Before
    fun before()
    {
        // 云存储测试需要真实凭据（CI 通过 -Ds3.* / -Dcos.* / -Dalioss.* 注入），
        // 未配置对应 endpoint 时跳过，而不是让整个测试套件失败
        Assume.assumeTrue(
            "Cloud storage credentials for [$pluginPrefix] are not configured, skipping",
            System.getProperty("$pluginPrefix.endpoint") != null
        )
        request.apply {
            access = System.getProperty("$pluginPrefix.access")
            secret = System.getProperty("$pluginPrefix.secret")
            bucket = System.getProperty("$pluginPrefix.bucket")
            endpoint = System.getProperty("$pluginPrefix.endpoint")
            fileName = this@BaseIOUtilsTest.fileName
        }
    }

    @Test
    fun step1_copy()
    {
        FileInputStream("src/test/kotlin/org/devlive/metaforge/test/BaseIOUtilsTest.kt").use { stream ->
            val copyMethod = ioUtils.getMethod("copy", FsRequest::class.java, InputStream::class.java, String::class.java)
            val response = copyMethod.invoke(null, request, stream, fileName)
            assertNotNull(response)
            log.info("Copy response [ {} ]", response)
        }
    }

    @Test
    fun step2_reader()
    {
        val readerMethod = ioUtils.getMethod("reader", FsRequest::class.java)
        val result = readerMethod.invoke(null, request)
        log.info("Reader result [ {} ]", result)
        assertNotNull(result)
        val inputStream = result as InputStream
        assertNotNull(inputStream.readBytes())
    }

    @Test
    fun step3_delete()
    {
        val deleteMethod = ioUtils.getMethod("delete", FsRequest::class.java)
        assertTrue(deleteMethod.invoke(null, request) as Boolean)
    }
}
