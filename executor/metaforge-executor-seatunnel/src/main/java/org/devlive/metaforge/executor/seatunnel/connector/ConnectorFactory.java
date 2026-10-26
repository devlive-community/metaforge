package org.devlive.metaforge.executor.seatunnel.connector;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.executor.configure.ExecutorConfigure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConnectorFactory
{
    private ConnectorFactory()
    {}

    public static Connector createFormatter(String type, ExecutorConfigure configure)
    {
        ConnectorType connectorType = null;
        try {
            connectorType = ConnectorType.valueOf(type);
        }
        catch (IllegalArgumentException e) {
            log.debug("No exact match found for type: {}, using default connector", type);
        }

        Connector instance;
        if (connectorType == ConnectorType.ClickHouse) {
            log.info("Creating ClickHouse connector");
            instance = new ConnectorClickHouse(type, configure);
        }
        else if (connectorType == ConnectorType.Console) {
            log.info("Creating Console connector");
            instance = new ConnectorConsole(type, configure);
        }
        else if (connectorType == ConnectorType.Jdbc) {
            log.info("Creating Jdbc connector");
            instance = new ConnectorJdbc(type, configure);
        }
        else {
            log.info("Creating default connector for type: {}", type);
            instance = new Connector(type, configure, configure.getSupportOptions())
            {
                @Override
                public Map<String, Object> formatToMap()
                {
                    Map<String, Object> node = new ConcurrentHashMap<>();
                    node.put(this.type, formatToProperties(configure.getConfigure()));
                    return node;
                }
            };
        }
        return instance;
    }
}
