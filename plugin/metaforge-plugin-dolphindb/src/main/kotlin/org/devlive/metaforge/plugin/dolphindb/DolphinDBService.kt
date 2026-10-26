package org.devlive.metaforge.plugin.dolphindb

import org.devlive.metaforge.spi.PluginService

class DolphinDBService : PluginService
{
    override fun validator(): String?
    {
        return "select version() as version"
    }

    override fun driver(): String
    {
        return "com.dolphindb.jdbc.Driver"
    }

    override fun connectType(): String?
    {
        return "dolphindb"
    }
}
