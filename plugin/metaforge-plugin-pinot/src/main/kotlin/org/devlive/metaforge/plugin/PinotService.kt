package org.devlive.metaforge.plugin

import org.devlive.metaforge.spi.PluginService

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
}
