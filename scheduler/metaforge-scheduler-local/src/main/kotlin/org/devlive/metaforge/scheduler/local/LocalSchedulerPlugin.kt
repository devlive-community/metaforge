package org.devlive.metaforge.scheduler.local

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class LocalSchedulerPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.SCHEDULER
    }
}
