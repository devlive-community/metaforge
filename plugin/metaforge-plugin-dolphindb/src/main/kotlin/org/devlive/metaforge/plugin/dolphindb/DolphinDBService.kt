package org.devlive.metaforge.plugin.dolphindb

import org.devlive.metaforge.spi.PluginService

class DolphinDBService : PluginService
{
    override fun validator(): String?
    {
        // Without FROM the driver returns the whole column as one vector object; select from a one-row table
        return "select version() as version from table(1 as x)"
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
