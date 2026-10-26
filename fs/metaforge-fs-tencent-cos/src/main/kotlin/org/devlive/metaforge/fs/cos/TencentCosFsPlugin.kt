package org.devlive.metaforge.fs.cos

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class TencentCosFsPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.FILESYSTEM
    }
}
