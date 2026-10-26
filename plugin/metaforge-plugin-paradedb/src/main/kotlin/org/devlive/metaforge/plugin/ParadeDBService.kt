package org.devlive.metaforge.plugin

import org.devlive.metaforge.spi.PluginService

class ParadeDBService : PluginService
{
    override fun driver(): String
    {
        return "org.postgresql.Driver"
    }

    override fun connectType(): String
    {
        return "postgresql"
    }
}
