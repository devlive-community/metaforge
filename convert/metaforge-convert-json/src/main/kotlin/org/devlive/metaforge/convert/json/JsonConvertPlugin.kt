package org.devlive.metaforge.convert.json

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class JsonConvertPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.CONVERT
    }
}
