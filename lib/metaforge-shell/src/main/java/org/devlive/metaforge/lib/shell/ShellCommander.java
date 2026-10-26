package org.devlive.metaforge.lib.shell;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public abstract class ShellCommander
{
    protected final ShellConfigure configure;

    public ShellCommander(ShellConfigure configure)
    {
        this.configure = configure;
    }

    protected String getShellType()
    {
        return "/bin/sh";
    }

    protected Map<String, String> getEnvironment()
    {
        Map<String, String> environment = new HashMap<>();
        Properties properties = System.getProperties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            environment.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        environment.putAll(System.getenv());
        return environment;
    }

    public abstract ShellResponse execute();
}
