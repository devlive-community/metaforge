package org.devlive.metaforge.plugin.natived.kafka;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.adapter.Adapter;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class KafkaService
        implements PluginService
{
    private KafkaConnection connection;
    private Response response;

    @Override
    public String validator()
    {
        return "SHOW TOPICS";
    }

    @Override
    public void connect(RuntimeConfigure configure)
    {
        try {
            this.response = new Response();
            this.connection = new KafkaConnection(configure, this.response);
        }
        catch (Exception ex) {
            this.response.setIsConnected(Boolean.FALSE);
            this.response.setMessage(ex.getMessage());
        }
    }

    @Override
    public Response execute(String content)
    {
        if (ObjectUtils.isNotEmpty(this.connection)) {
            log.info("Execute kafka plugin logic started");
            this.response = this.connection.getResponse();
            Adapter processor = new KafkaAdapter(this.connection, new KafkaParser(content));
            this.response = processor.handlerExecute(content);
            log.info("Execute kafka plugin logic end");
        }
        return this.response;
    }

    @Override
    public Response execute(RuntimeConfigure configure, String content)
    {
        connect(configure);
        return execute(content);
    }

    @Override
    public void destroy()
    {
        if (ObjectUtils.isNotEmpty(this.connection)) {
            this.connection.destroy();
        }
    }
}
