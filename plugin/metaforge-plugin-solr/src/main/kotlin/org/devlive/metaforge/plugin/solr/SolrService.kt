package org.devlive.metaforge.plugin.solr

import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.connection.JdbcUrlGuard
import org.devlive.metaforge.spi.model.RuntimeConfigure

/**
 * Solr's SQL through the SolrCloud JDBC driver: the host and port are ZooKeeper's (9983 for
 * Solr's embedded ZooKeeper) and the database is the collection a connection opens.
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
}
