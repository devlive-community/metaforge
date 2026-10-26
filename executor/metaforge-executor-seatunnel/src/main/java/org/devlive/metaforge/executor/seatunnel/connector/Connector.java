package org.devlive.metaforge.executor.seatunnel.connector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.executor.configure.ExecutorConfigure;
import org.devlive.metaforge.spi.model.Configure;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public abstract class Connector
{
    protected final String type;
    protected final ExecutorConfigure configure;
    private final Set<String> supportOptions;

    public Connector(String type, ExecutorConfigure configure, Set<String> supportOptions)
    {
        this.type = type;
        this.configure = configure;
        this.supportOptions = supportOptions;
        this.checkSupportOptions();
    }

    private void checkSupportOptions()
    {
        Properties originalConfigure = configure.getConfigure();
        Preconditions.checkArgument(originalConfigure != null, "Original configure must be not empty");

        for (String option : supportOptions) {
            if (!originalConfigure.containsKey(option)) {
                Preconditions.checkArgument(false, String.format("Type [ %s ] option [ %s ] is not supported", type, option));
            }
        }
    }

    public Map<String, Object> formatToMap()
    {
        Map<String, Object> node = Maps.newConcurrentMap();
        node.put(this.type, formatToProperties(configure.getConfigure()));
        return node;
    }

    protected Properties formatToProperties(Properties originalConfigure)
    {
        Properties properties = new Properties();
        if (originalConfigure != null) {
            originalConfigure.entrySet()
                    .stream()
                    .filter(entry -> !String.valueOf(entry.getValue()).equals("None"))
                    .forEach(entry -> properties.put(entry.getKey(), entry.getValue()));
        }
        return properties;
    }

    protected Properties formatToProperties(Configure originalConfigure)
    {
        return new Properties();
    }
}
