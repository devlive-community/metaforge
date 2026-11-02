package org.devlive.metaforge.test.metaforge

import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaForgeExecutorPluginTest
{
    private val pluginManager: PluginManager
    private val pluginName = "MetaForge"

    init
    {
        val config = PluginConfigure.builder()
            .pluginsDir(moduleDir())
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    /** 从当前目录向上找到仓库里的执行器模块；Maven 下当前目录是测试模块而不是仓库根。 */
    private fun moduleDir(): Path
    {
        var dir: Path? = Paths.get("").toAbsolutePath()
        while (dir != null)
        {
            val candidate = dir.resolve("executor/metaforge-executor-metaforge")
            if (Files.isRegularFile(candidate.resolve("pom.xml")))
            {
                return candidate
            }
            dir = dir.parent
        }
        throw IllegalStateException("executor/metaforge-executor-metaforge not found above the working directory")
    }

    @Test
    fun test()
    {
        assertTrue(pluginManager.getPlugin(pluginName).isPresent)
        assertEquals(pluginName, pluginManager.getPlugin(pluginName).get().name)
    }

    @Test
    fun formerNamesResolveToTheMetaForgeExecutor()
    {
        assertEquals(pluginName, pluginManager.getPlugin("Local").get().name)
        assertEquals(pluginName, pluginManager.getPlugin("LocalExecutor").get().name)
    }
}
