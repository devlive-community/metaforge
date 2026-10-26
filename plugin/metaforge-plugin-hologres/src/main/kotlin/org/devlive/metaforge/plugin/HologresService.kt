package org.devlive.metaforge.plugin

import org.devlive.metaforge.spi.PluginService

class HologresService : PluginService
{
    override fun connectType(): String
    {
        return "postgresql"
    }

    override fun driver(): String
    {
        return "org.postgresql.Driver"
    }
}