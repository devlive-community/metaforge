package org.devlive.metaforge.convert.none

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class NoneConvertPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.CONVERT
    }
}
