package org.devlive.metaforge.convert

import org.devlive.metaforge.plugin.PluginManager
import java.util.*

object ConvertFilter
{
    /**
     * Finds a file in the injector by name.
     *
     * @param pluginManager PluginManager
     * @param name the name of the file to find
     * @return an Optional containing the found File, or an empty Optional if not found
     */
    @JvmStatic
    fun filter(pluginManager: PluginManager, name: String): Optional<ConvertService>
    {
        return pluginManager.getPlugin(name)
            .map { it.getService(ConvertService::class.java) }
    }
}
