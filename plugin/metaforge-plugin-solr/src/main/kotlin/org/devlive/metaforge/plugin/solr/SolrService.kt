package org.devlive.metaforge.plugin.solr

import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.connection.JdbcUrlGuard
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.RuntimeConfigure

/**
 * Solr's SQL through the SolrCloud JDBC driver: the host and port are ZooKeeper's (9983 for
 * Solr's embedded ZooKeeper) and the database is the collection a connection opens. Collections
 * are tables of one database.
 */
class SolrService : PluginService
{
    override fun validator(): String
    {
        return "SELECT '-' AS version"
    }

    override fun driver(): String
    {
        return "org.apache.solr.client.solrj.io.sql.DriverImpl"
    }

    override fun connectType(): String
    {
        return "solr"
    }

    /** The driver takes the collection as a parameter; a path after the address would be a ZooKeeper chroot. */
    override fun url(configure: RuntimeConfigure): String
    {
        if (configure.url.isPresent)
        {
            return configure.url.get()
        }
        val parameters = mutableListOf<String>()
        configure.database.ifPresent { collection ->
            JdbcUrlGuard.validateParameter("collection", collection)
            parameters.add("collection=$collection")
        }
        configure.env.ifPresent { env ->
            env.forEach { (key, value) ->
                JdbcUrlGuard.validateParameter(key, value.toString())
                parameters.add("$key=$value")
            }
        }
        val url = "jdbc:solr://${JdbcUrlGuard.validateHost(configure.host)}:${configure.port}"
        return if (parameters.isEmpty()) url else url + "?" + parameters.joinToString("&")
    }

    override fun getDatabases(configure: RuntimeConfigure): Response
    {
        return this.execute(configure, "SELECT 'default' AS object_name, 'DATABASE' AS object_type, '' AS object_comment")
    }

    override fun getTables(configure: RuntimeConfigure, database: String): Response
    {
        return this.execute(configure, "SELECT 'table' AS type_name, tableName AS object_name, '' AS object_comment " +
                "FROM metadata.TABLES WHERE tableType = 'TABLE'")
    }

    override fun getColumns(configure: RuntimeConfigure, database: String, table: String): Response
    {
        return this.execute(configure, "SELECT 'column' AS type_name, columnName AS object_name, typeName AS object_data_type, " +
                "isNullable AS object_nullable, ordinalPosition AS object_position " +
                "FROM metadata.COLUMNS WHERE tableName = '${table.replace("'", "''")}'")
    }
}
