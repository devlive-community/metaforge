package org.devlive.metaforge.service.common;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.devlive.metaforge.spi.PluginService;

import java.util.Optional;
import java.util.Set;

public class PluginUtils
{
    private PluginUtils() {}

    public static Optional<PluginService> getPluginByName(Injector injector, String pluginName)
    {
        return injector.getInstance(Key.get(new TypeLiteral<Set<PluginService>>() {}))
                .stream()
                .filter(plugin -> plugin.name().equalsIgnoreCase(pluginName))
                .findFirst();
    }

    public static Optional<PluginService> getPluginByNameAndType(Injector injector, String pluginName, String pluginType)
    {
        return injector.getInstance(Key.get(new TypeLiteral<Set<PluginService>>() {}))
                .stream()
                .filter(plugin -> plugin.name().equalsIgnoreCase(pluginName) && plugin.type().name().equalsIgnoreCase(pluginType))
                .findFirst();
    }
}
