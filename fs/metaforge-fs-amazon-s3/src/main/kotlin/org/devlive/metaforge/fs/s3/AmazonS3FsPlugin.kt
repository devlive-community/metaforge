package org.devlive.metaforge.fs.s3

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class AmazonS3FsPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.FILESYSTEM
    }
}
