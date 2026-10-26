package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.connection.HttpConfigure;
import org.devlive.metaforge.spi.connection.HttpConnection;
import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Response;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class QuestDBService
        implements PluginService
{
    private HttpConfigure configure;
    private HttpConnection connection;
    private Response response;

    @Override
    public void connect(Configure configure)
    {
        try {
            this.response = new Response();
            this.configure = new HttpConfigure();
            BeanUtils.copyProperties(this.configure, configure);
            this.connection = new HttpConnection(this.configure, this.response);
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
            log.info("Execute questdb plugin logic started");
            this.response = this.connection.getResponse();
            QuestDBAdapter processor = new QuestDBAdapter(this.connection);
            this.response = processor.handlerExecute(content);
            log.info("Execute questdb plugin logic end");
        }
        this.destroy();
        return this.response;
    }

    @Override
    public Response execute(Configure configure, String content)
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
