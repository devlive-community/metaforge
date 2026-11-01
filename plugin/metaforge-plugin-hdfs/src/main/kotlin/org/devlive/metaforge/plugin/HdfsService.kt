package org.devlive.metaforge.plugin

import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.RuntimeConfigure

/**
 * HDFS through MetaForge's HDFS JDBC driver: a database is a directory under the root, a table a
 * directory inside it, and a table's rows its entries with fixed attribute columns. Databases,
 * tables and columns come from the driver's JDBC metadata.
 */
class HdfsService : PluginService
{
    override fun driver(): String
    {
        return "org.devlive.metaforge.HdfsDriver"
    }

    override fun connectType(): String
    {
        return "hdfs"
    }

    /** jdbc:hdfs://namenode:port with the custom options as Hadoop configuration; the user goes as user=. */
    override fun url(configure: RuntimeConfigure): String
    {
        if (configure.url.isPresent)
        {
            return configure.url.get()
        }
        val options = LinkedHashMap<String, Any>()
        configure.env.ifPresent { options.putAll(it) }
        configure.username.filter { it.isNotEmpty() }.ifPresent { options["user"] = it }
        val host = configure.host
        val address = if (configure.port != null && ! host.contains(":")) "$host:${configure.port}" else host
        val query = options.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "jdbc:hdfs://$address" + if (query.isEmpty()) "" else "?$query"
    }

    override fun getDatabases(configure: RuntimeConfigure): Response
    {
        return metadataDatabases(configure)
    }

    override fun getTables(configure: RuntimeConfigure, database: String): Response
    {
        return metadataTables(configure, database)
    }

    override fun getColumns(configure: RuntimeConfigure, database: String, table: String): Response
    {
        return metadataColumns(configure, database, table)
    }
}
