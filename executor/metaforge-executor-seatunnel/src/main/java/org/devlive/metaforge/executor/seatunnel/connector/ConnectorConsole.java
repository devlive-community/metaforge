package org.devlive.metaforge.executor.seatunnel.connector;

import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.executor.configure.ExecutorConfigure;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectorConsole
        extends Connector
{
    private static final Set<String> supportOptions = new HashSet<>();

    public ConnectorConsole(String type, ExecutorConfigure configure)
    {
        super(type, configure, supportOptions);
    }

    @Override
    public Map<String, Object> formatToMap()
    {
        Map<String, Object> node = new ConcurrentHashMap<>();
        Properties properties = this.configure.getConfigure();
        if (ObjectUtils.isEmpty(properties)) {
            properties = new Properties();
        }
        node.put("Console", properties);
        return node;
    }
}
