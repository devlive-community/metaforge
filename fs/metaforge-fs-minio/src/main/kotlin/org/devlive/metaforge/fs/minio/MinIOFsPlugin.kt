package org.devlive.metaforge.fs.minio

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class MinIOFsPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.FILESYSTEM
    }
}
