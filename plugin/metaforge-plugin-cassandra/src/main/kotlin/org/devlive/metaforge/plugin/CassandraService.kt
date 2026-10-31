package org.devlive.metaforge.plugin

import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.RuntimeConfigure

/**
 * Cassandra through the cassandra-jdbc-wrapper JDBC driver, speaking CQL: a keyspace is a database.
 * Keyspaces, tables and columns come from the driver's JDBC metadata.
 */
class CassandraService : PluginService
{
    override fun driver(): String
    {
        return "com.ing.data.cassandra.jdbc.CassandraDriver"
    }

    override fun connectType(): String
    {
        return "cassandra"
    }

    override fun validator(): String
    {
        return "SELECT release_version AS version FROM system.local"
    }

    /** The driver needs the local data center; datacenter1 unless the custom options name another. */
    override fun url(configure: RuntimeConfigure): String
    {
        if (configure.url.isPresent)
        {
            return configure.url.get()
        }
        val options = LinkedHashMap<String, Any>()
        options["localdatacenter"] = "datacenter1"
        configure.env.ifPresent { options.putAll(it) }
        val keyspace = configure.database.filter { it.isNotEmpty() }.map { "/$it" }.orElse("")
        return "jdbc:cassandra://${configure.host}:${configure.port}$keyspace?" + options.entries.joinToString("&") { "${it.key}=${it.value}" }
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
