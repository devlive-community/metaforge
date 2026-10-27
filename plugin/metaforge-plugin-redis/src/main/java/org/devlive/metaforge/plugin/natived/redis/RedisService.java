package org.devlive.metaforge.plugin.natived.redis;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.adapter.Adapter;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class RedisService
        implements PluginService
{
    private RedisConnection connection;
    private Response response;

    @Override
    public String validator()
    {
        return "PING Redis";
    }

    @Override
    public void connect(RuntimeConfigure configure)
    {
        try {
            this.response = new Response();
            this.connection = new RedisConnection(configure, this.response);
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
            log.info("Execute redis plugin logic started");
            this.response = this.connection.getResponse();
            Adapter processor = new RedisAdapter(this.connection);
            this.response = processor.handlerExecute(content);
            log.info("Execute redis plugin logic end");
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
