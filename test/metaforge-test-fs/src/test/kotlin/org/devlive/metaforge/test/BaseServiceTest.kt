package org.devlive.metaforge.test

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.fs.FsRequest
import org.devlive.metaforge.fs.FsService
import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.junit.Assert
import org.junit.Assume
import org.junit.Assert.assertNotNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressFBWarnings(value = ["OBL_UNSATISFIED_OBLIGATION"])
abstract class BaseServiceTest(
    private val pluginName: String,
    pluginHome: String,
    pluginPrefix: String
)
{
    private val log = LoggerFactory.getLogger(this::class.java)
    private var available = false
    private val pluginManager: PluginManager
    private val fileName = "${pluginName}PluginTest.kt"
    private val request: FsRequest

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve(pluginHome))
            .build()

        pluginManager = PluginManager(config).apply { start() }

        val localPath = Paths.get(
            System.getProperty("user.dir"),
            "src", "test", "kotlin", "io", "edurt", "metaforge", "test", "BaseServiceTest.kt"
        ).toString()
        log.info("local path [ {} ]", localPath)

        var endpoint = System.getProperty("$pluginPrefix.endpoint")
        if (endpoint == null)
        {
            val tempDir: Path = Paths.get(System.getProperty("user.dir"), "data")
            endpoint = tempDir.toString()
        }
        log.info("endpoint [ {} ]", endpoint)

        // 云存储插件测试依赖打包后的插件 jar 与真实凭证；CI（仅 test-compile、无凭证）跳过
        available = pluginManager.getPlugin(pluginName).isPresent
        val credentialsPresent = System.getProperty("$pluginPrefix.access") != null
        Assume.assumeTrue(
            "$pluginName plugin or credentials are not available in this environment, skipping",
            available && credentialsPresent
        )

        request = FsRequest.builder()
            .access(System.getProperty("$pluginPrefix.access"))
            .secret(System.getProperty("$pluginPrefix.secret"))
            .bucket(System.getProperty("$pluginPrefix.bucket"))
            .endpoint(endpoint)
            .stream(FileInputStream(localPath))
            .fileName(fileName)
            .build()
    }

    private fun getPlugin(): Optional<Plugin>
    {
        return pluginManager.getPlugin(pluginName)
    }

    @Test
    fun step1_plugin()
    {
        Assume.assumeTrue(available)
        val plugin = pluginManager.getPlugin(pluginName)
        assertNotNull(plugin.get())
    }

    @Test
    fun step2_writer()
    {
        val plugin = getPlugin()
        plugin.ifPresent {
            val service = it.getService(FsService::class.java)
            val response = service.writer(request)
            log.info("Writer response [ {} ]", response)
            assertTrue(response.isSuccessful)
        }
    }

    @Test
    fun step3_reader()
    {
        val plugin = getPlugin()
        plugin.ifPresent {
            val service = it.getService(FsService::class.java)
            val response = service.reader(request)
            log.info("$pluginName reader response [ {} ]", response)
            assertTrue(response.isSuccessful)
        }
    }

    @Test
    fun step4_delete()
    {
        val plugin = getPlugin()
        plugin.ifPresent {
            val service = it.getService(FsService::class.java)
            val response = service.delete(request)
            log.info("$pluginName delete response [ {} ]", response)
            assertTrue(response.isSuccessful)
        }
    }
}
