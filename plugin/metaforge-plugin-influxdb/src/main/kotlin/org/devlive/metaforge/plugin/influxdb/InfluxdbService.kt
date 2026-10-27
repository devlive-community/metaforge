package org.devlive.metaforge.plugin.influxdb

import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.RuntimeConfigure

class InfluxdbService : PluginService
{
    override fun validator(): String
    {
        return "SELECT 1"
    }

    override fun driver(): String
    {
        return "net.suteren.jdbc.influxdb.InfluxDbDriver"
    }

    override fun connectType(): String
    {
        return "influxdb"
    }

    override fun url(configure: RuntimeConfigure): String
    {
        // 如果已经有自定义 URL，直接使用
        // If there is a custom URL, use it
        if (configure.url.isPresent)
        {
            return configure.url.get()
        }

        val builder = StringBuilder("jdbc:${configure.type}:")

        // 添加主机和端口
        // Add host and port
        builder.append(configure.host)
        if (configure.port != null)
        {
            builder.append(":").append(configure.port)
        }
        builder.append("/")

        // 添加参数
        // Add parameters
        val params = mutableListOf<String>()

        // 添加数据库
        // Add database
        configure.database.ifPresent { db ->
            params.add("db=$db")
        }

        // 添加用户名和密码
        // Add username and password
        configure.username.ifPresent { username ->
            params.add("u=$username")
        }
        configure.password.ifPresent { password ->
            params.add("p=$password")
        }

        // 添加其他环境参数
        // Add other environment parameters
        configure.env.ifPresent { envMap ->
            envMap.forEach { (key, value) ->
                params.add("$key=$value")
            }
        }

        // 将所有参数添加到 URL
        // Add all parameters to the URL
        if (params.isNotEmpty())
        {
            builder.append("?").append(params.joinToString("&"))
        }

        return builder.toString()
    }
}
