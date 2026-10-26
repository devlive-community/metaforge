package org.devlive.metaforge.test.local

import com.google.common.collect.Sets
import org.devlive.metaforge.executor.ExecutorService
import org.devlive.metaforge.executor.configure.ExecutorConfigure
import org.devlive.metaforge.executor.configure.ExecutorRequest
import org.devlive.metaforge.executor.configure.OriginColumn
import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Configure
import junit.framework.TestCase.assertNotNull
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import java.nio.file.Path
import java.util.*

class LocalExecutorServiceTest
{
    companion object
    {
        @BeforeClass
        @JvmStatic
        fun requireRemoteArtifact()
        {
            // 测试依赖从外部 CDN 下载 2024.4.0-SNAPSHOT 的历史插件包，当前插件体系已无法识别。
            // 默认跳过以保证 CI 稳定；需要时通过 -Dmetaforge.test.remotePlugin=true 显式开启。
            Assume.assumeTrue("Remote CDN plugin artifact is not available", java.lang.Boolean.getBoolean("metaforge.test.remotePlugin"))
        }
    }

    private val pluginManager: PluginManager
    private val pluginName = "Local"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("executor/metaforge-executor-local"))
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun testStartSuccess()
    {
        pluginManager.installPlugin(
            Path.of("https://cdn.north.devlive.org/applications/metaforge/plugins/2024.4.0-SNAPSHOT/plugin/metaforge-plugin-mysql-bin.tar.gz"),
            "metaforge-plugin-mysql"
        )

        val pluginService = pluginManager.getPlugin("MySQL")
            .get()
            .getService(PluginService::class.java)

        val inputConfigure = Configure()
        inputConfigure.host = "localhost"
        inputConfigure.port = 5432
        inputConfigure.username = Optional.of("test")
        inputConfigure.password = Optional.of("test")

        val inputProps = Properties().apply {
            setProperty("host", "localhost")
            setProperty("port", "5432")
            setProperty("username", "test")
            setProperty("password", "test")
        }

        val originColumns = Sets.newHashSet<OriginColumn>().apply {
            add(OriginColumn("id", "id"))
            add(OriginColumn("name", "name"))
        }

        val inputExecutorConfigure = ExecutorConfigure(
            type = "TestInput",
            configure = inputProps,
            supportOptions = setOf("option1", "option2")
        ).apply {
            plugin = pluginService
            query = "SELECT id, name FROM test_table"
            database = "test_db"
            table = "test_table"
            originConfigure = inputConfigure
            this.originColumns = originColumns
        }

        val outputConfigure = Configure()
        outputConfigure.host = "localhost"
        outputConfigure.port = 3306
        inputConfigure.username = Optional.of("test")
        inputConfigure.password = Optional.of("test")

        val outputProps = Properties().apply {
            setProperty("host", "localhost")
            setProperty("port", "3306")
            setProperty("username", "test")
            setProperty("password", "test")
        }

        val outputExecutorConfigure = ExecutorConfigure(
            type = "TestOutput",
            configure = outputProps,
            supportOptions = setOf("option1", "option2")
        ).apply {
            plugin = pluginService
            database = "test_db"
            table = "test_table"
            originConfigure = outputConfigure
        }

        val request = ExecutorRequest(
            workHome = "/tmp/test",
            input = inputExecutorConfigure,
            output = outputExecutorConfigure
        )

        val response = pluginManager.getPlugin(pluginName)
            .get()
            .getService(ExecutorService::class.java)
            .start(request)

        assertNotNull(response)
    }
}
