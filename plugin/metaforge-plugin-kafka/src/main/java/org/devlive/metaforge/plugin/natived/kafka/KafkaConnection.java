package org.devlive.metaforge.plugin.natived.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.devlive.metaforge.spi.connection.Connection;
import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Response;

import java.util.Properties;

@Slf4j
public class KafkaConnection
        extends Connection
{
    private Configure configure;
    private Response response;

    @Getter
    private AdminClient client;

    public KafkaConnection(Configure configure, Response response)
    {
        super(configure, response);
    }

    @Override
    protected java.sql.Connection openConnection()
    {
        try {
            this.configure = getConfigure();
            this.response = getResponse();
            Properties properties = new Properties();
            properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, configure.getHost());
            this.client = AdminClient.create(properties);
            response.setIsConnected(Boolean.TRUE);
        }
        catch (Exception ex) {
            log.error("Connection failed ", ex);
            response.setIsConnected(Boolean.FALSE);
            response.setMessage(ex.getMessage());
        }
        return null;
    }

    @Override
    public void destroy()
    {
        this.client.close();
        log.info("Connection close successful");
    }
}
