package org.devlive.metaforge.executor.workflow

import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

/**
 * 执行器工作流声明文件的位置。每个执行器插件把声明放在自己 jar（或开发时 classes 目录）的
 * 固定路径 [FILE]，并按服务类的 CodeSource 定位，不按资源名全局查找——多个插件共享类路径时
 * 按名字查找会读到别人的声明。声明里引用的节点文件按相对路径解析。
 *
 * Locates the workflow declaration an executor plugin ships at [FILE] inside its own jar or classes
 * directory. It is resolved from the service class's code source, never by a global resource lookup,
 * so plugins sharing a class path cannot read each other's declarations.
 */
object WorkflowDeclaration
{
    const val FILE = "org/devlive/metaforge/executor/workflow/workflow.yaml"

    @JvmStatic
    fun locate(type: Class<*>): URL?
    {
        // A service extending another executor's service inherits its declaration unless it ships one.
        var owner: Class<*>? = type
        while (owner != null && owner != Any::class.java)
        {
            val url = declaration(owner)
            if (url != null && exists(url))
            {
                return url
            }
            owner = owner.superclass
        }
        return null
    }

    private fun declaration(type: Class<*>): URL?
    {
        val location = type.protectionDomain?.codeSource?.location ?: return null
        return try
        {
            val text = location.toString()
            if (text.endsWith(".jar")) URL("jar:$text!/$FILE") else URL(location, FILE)
        }
        catch (_: MalformedURLException)
        {
            null
        }
    }

    private fun exists(url: URL): Boolean =
        try
        {
            val connection = url.openConnection()
            // Plugins are reloaded from their jars; a cached jar would stay open and stale.
            connection.useCaches = false
            connection.getInputStream().use { true }
        }
        catch (_: IOException)
        {
            false
        }
}
