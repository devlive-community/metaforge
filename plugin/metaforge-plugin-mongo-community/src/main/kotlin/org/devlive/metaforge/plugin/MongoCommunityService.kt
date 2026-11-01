package org.devlive.metaforge.plugin

import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.RuntimeConfigure

class MongoCommunityService : PluginService
{
    override fun connectType(): String
    {
        return "mongodb"
    }

    // jdbc:mongodb://host:port/database: the driver reads the database from the path.
    override fun url(configure: RuntimeConfigure): String
    {
        val buffer = StringBuilder()
        buffer.append("jdbc:")
        buffer.append(configure.type)
        buffer.append("://")
        buffer.append(configure.host)
        buffer.append(":")
        buffer.append(configure.port)
        configure.database.filter { it.isNotBlank() }.ifPresent { buffer.append("/").append(it) }
        return buffer.toString()
    }

    override fun driver(): String
    {
        return "org.devlive.metaforge.MongoDriver"
    }

    // Databases, collections and the fields found in their documents come from the driver's JDBC metadata.
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
