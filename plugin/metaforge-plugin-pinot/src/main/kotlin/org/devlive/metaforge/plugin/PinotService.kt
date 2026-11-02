package org.devlive.metaforge.plugin

import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.RuntimeConfigure

class PinotService : PluginService
{
    override fun validator(): String
    {
        // Pinot SQL has no version function; select a constant, which needs no table
        return "SELECT '-' AS version"
    }

    override fun connectType(): String
    {
        return "pinot"
    }

    override fun driver(): String
    {
        return "org.apache.pinot.client.PinotDriver"
    }

    // Pinot's driver reports no schemas or catalogs: tables live in one database, and table and column lookups take no schema

    override fun getDatabases(configure: RuntimeConfigure): Response
    {
        return this.execute(configure, "SELECT 'default' AS object_name, 'DATABASE' AS object_type, '' AS object_comment")
    }

    override fun getTables(configure: RuntimeConfigure, database: String?): Response
    {
        return metadataTables(configure, null)
    }

    override fun getColumns(configure: RuntimeConfigure, database: String?, table: String): Response
    {
        return metadataColumns(configure, null, table)
    }
}
