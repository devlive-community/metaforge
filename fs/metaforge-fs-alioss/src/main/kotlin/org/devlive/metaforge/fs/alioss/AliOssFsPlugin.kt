package org.devlive.metaforge.fs.alioss

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class AliOssFsPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.FILESYSTEM
    }
}
