package org.devlive.metaforge.plugin;

public enum SqlServerVersion
{
    V2017("2017"),
    V2019("2019"),
    V2022("2022");

    private String version;

    SqlServerVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return this.version;
    }
}
