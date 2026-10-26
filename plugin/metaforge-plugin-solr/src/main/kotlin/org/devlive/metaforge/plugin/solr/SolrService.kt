package org.devlive.metaforge.plugin.solr

import org.devlive.metaforge.spi.PluginService

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
}
