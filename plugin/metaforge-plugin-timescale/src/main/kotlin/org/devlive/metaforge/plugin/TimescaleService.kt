package org.devlive.metaforge.plugin

import org.devlive.metaforge.spi.PluginService

class TimescaleService : PluginService
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
