package org.devlive.metaforge.convert.xml

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class XmlConvertPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.CONVERT
    }
}
