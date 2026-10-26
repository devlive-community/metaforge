package org.devlive.metaforge.convert.csv

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class CsvConvertPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.CONVERT
    }
}
