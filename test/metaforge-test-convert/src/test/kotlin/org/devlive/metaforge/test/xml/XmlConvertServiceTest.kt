package org.devlive.metaforge.test.xml

import org.devlive.metaforge.convert.model.ConvertRequest
import org.devlive.metaforge.convert.xml.XmlConvertService
import org.devlive.metaforge.plugin.PluginConfigure
import org.devlive.metaforge.plugin.PluginManager
import org.devlive.metaforge.plugin.utils.PluginPathUtils
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import kotlin.test.assertTrue

class XmlConvertServiceTest
{
    private val pluginManager: PluginManager
    private val pluginName = "XmlConvert"
    private val request: ConvertRequest

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("convert/metaforge-convert-xml"))
            .scanDepth(2)
            .build()

        pluginManager = PluginManager(config).apply { start() }

        request = ConvertRequest()
        request.name = "test"
        request.path = System.getProperty("user.dir")
        request.headers = listOf("name", "age")
        val l1 = listOf("Test", 12)
        val l2 = listOf("Test1", 121)
        request.columns = listOf(l1, l2)
    }

    @Test
    fun test()
    {
        pluginManager.getPlugin(pluginName).ifPresent { plugin ->
            val service = plugin.getService(XmlConvertService::class.java)
            assertNotNull(service)
        }
    }

    @Test
    fun testWriter()
    {
        pluginManager.getPlugin(pluginName).ifPresent { plugin ->
            val service = plugin.getService(XmlConvertService::class.java)

            val response = service.writer(request)
            response.successful?.let { assertTrue(it) }
        }
    }
}
