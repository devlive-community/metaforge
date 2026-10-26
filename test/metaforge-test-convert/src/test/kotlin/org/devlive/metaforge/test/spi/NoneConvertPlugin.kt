package org.devlive.metaforge.test.spi

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType
import org.devlive.metaforge.plugin.annotation.InjectPlugin

@InjectPlugin
class NoneConvertPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.CONVERT
    }
}
