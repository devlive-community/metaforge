package org.devlive.metaforge.plugin;

import lombok.Getter;

@Getter
public enum PluginType
{
    CONNECTOR("Connector"),
    EXECUTOR("Executor"),
    SCHEDULER("Scheduler"),
    CONVERT("Convert"),
    FILESYSTEM("FileSystem"),
    NOTIFY("Notify");

    private final String name;

    PluginType(String name)
    {
        this.name = name;
    }
}
