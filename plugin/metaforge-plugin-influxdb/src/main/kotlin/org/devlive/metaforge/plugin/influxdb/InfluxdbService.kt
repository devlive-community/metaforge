package org.devlive.metaforge.plugin.influxdb

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.RuntimeConfigure

class InfluxdbService : PluginService
{
    override fun validator(): String
    {
        // InfluxQL needs FROM in every SELECT; the diagnostics start with the build: branch, commit and version
        return "SHOW DIAGNOSTICS"
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

    // InfluxDB has no information_schema: databases, measurements, fields and tags come from SHOW statements,
    // whose headers can not be renamed in InfluxQL

    override fun getDatabases(configure: RuntimeConfigure): Response
    {
        return remap(this.execute(configure, "SHOW DATABASES"), DATABASE_HEADERS) { mapper, source, _ ->
            mapper.createObjectNode()
                .put("object_name", source.path("name").asText())
                .put("object_type", "DATABASE")
                .put("object_comment", "")
        }
    }

    override fun getTables(configure: RuntimeConfigure, database: String): Response
    {
        return remap(this.execute(configure, "SHOW MEASUREMENTS ON ${quote(database)}"), TABLE_HEADERS) { mapper, source, _ ->
            mapper.createObjectNode()
                .put("type_name", "table")
                .put("object_name", source.path("name").asText())
                .put("object_comment", "")
        }
    }

    override fun getColumns(configure: RuntimeConfigure, database: String, table: String): Response
    {
        val tags = remap(this.execute(configure, "SHOW TAG KEYS ON ${quote(database)} FROM ${quote(table)}"), COLUMN_HEADERS) { mapper, source, position ->
            column(mapper, source.path("tagKey").asText(), "string", "TAG", position)
        }
        if (tags.isSuccessful != true)
        {
            return tags
        }
        val fields = remap(this.execute(configure, "SHOW FIELD KEYS ON ${quote(database)} FROM ${quote(table)}"), COLUMN_HEADERS) { mapper, source, position ->
            column(mapper, source.path("fieldKey").asText(), source.path("fieldType").asText(), "FIELD", tags.columns.size + position)
        }
        if (fields.isSuccessful == true)
        {
            fields.columns = tags.columns + fields.columns
        }
        return fields
    }

    private fun column(mapper: ObjectMapper, name: String, type: String, definition: String, position: Int): ObjectNode
    {
        return mapper.createObjectNode()
            .put("type_name", "column")
            .put("object_name", name)
            .put("object_data_type", type)
            .put("object_nullable", "YES")
            .put("object_default_value", "")
            .put("object_comment", "")
            .put("object_position", position)
            .put("object_definition", definition)
    }

    private fun quote(identifier: String): String
    {
        return "\"" + identifier.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    companion object
    {
        private val DATABASE_HEADERS = listOf("object_name", "object_type", "object_comment")
        private val TABLE_HEADERS = listOf("type_name", "object_name", "object_comment")
        private val COLUMN_HEADERS = listOf("type_name", "object_name", "object_data_type", "object_nullable",
                                            "object_default_value", "object_comment", "object_position", "object_definition")

        /** Gives a statement's JSON rows the headers MetaForge reads; other formats are returned as they are. */
        private fun remap(response: Response, headers: List<String>, mapper: (ObjectMapper, JsonNode, Int) -> ObjectNode): Response
        {
            if (response.isSuccessful != true || response.columns.any { it !is JsonNode })
            {
                return response
            }
            val json = ObjectMapper()
            val rows = mutableListOf<Any>()
            response.columns.forEach { rows.add(mapper(json, it as JsonNode, rows.size + 1)) }
            response.headers = headers
            response.columns = rows
            return response
        }
    }
}
