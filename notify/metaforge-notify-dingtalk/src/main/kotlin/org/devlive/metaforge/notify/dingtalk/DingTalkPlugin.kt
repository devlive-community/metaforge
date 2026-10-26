package org.devlive.metaforge.notify.dingtalk

import org.devlive.metaforge.plugin.Plugin
import org.devlive.metaforge.plugin.PluginType

class DingTalkPlugin : Plugin()
{
    override fun getType(): PluginType
    {
        return PluginType.NOTIFY
    }
}
