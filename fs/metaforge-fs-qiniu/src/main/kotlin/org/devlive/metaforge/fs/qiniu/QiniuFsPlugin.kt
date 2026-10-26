package org.devlive.metaforge.fs.qiniu

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class QiniuFsPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.FILESYSTEM
    }
}
