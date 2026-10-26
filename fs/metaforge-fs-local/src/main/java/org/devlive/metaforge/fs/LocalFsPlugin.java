package org.devlive.metaforge.fs;

import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginType;

public class LocalFsPlugin
        extends Plugin
{
    @Override
    public PluginType getType()
    {
        return PluginType.FILESYSTEM;
    }
}
