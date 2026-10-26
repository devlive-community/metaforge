package org.devlive.metaforge.convert

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class TxtConvertPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.CONVERT
    }
}
