package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.adapter.Adapter;
import org.devlive.metaforge.spi.connection.HttpConfigure;
import org.devlive.metaforge.spi.connection.HttpConnection;
import org.devlive.metaforge.spi.model.Configure;
import org.devlive.metaforge.spi.model.Response;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class ClickHouseHTTPService
        implements PluginService
{
    private HttpConfigure httpConfigure;
    private HttpConnection connection;
    private Response response;

    @Override
    public void connect(Configure configure)
    {
        try {
            this.response = new Response();
            this.httpConfigure = new HttpConfigure();
            BeanUtils.copyProperties(this.httpConfigure, configure);
            this.connection = new ClickHouseHTTPConnection(this.httpConfigure, this.response);
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
            log.info("Execute clickhouse plugin logic started");
            this.response = this.connection.getResponse();
            Adapter processor = new ClickHouseHTTPAdapter(this.connection);
            this.response = processor.handlerExecute(content);
            log.info("Execute clickhouse plugin logic end");
        }
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
