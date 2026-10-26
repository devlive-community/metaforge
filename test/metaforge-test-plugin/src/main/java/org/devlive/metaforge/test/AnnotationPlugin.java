package org.devlive.metaforge.test;

import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginType;
import org.devlive.metaforge.plugin.annotation.InjectPlugin;

@InjectPlugin
public class AnnotationPlugin
        extends Plugin
{
    @Override
    public PluginType getType()
    {
        return PluginType.CONNECTOR;
    }
}
