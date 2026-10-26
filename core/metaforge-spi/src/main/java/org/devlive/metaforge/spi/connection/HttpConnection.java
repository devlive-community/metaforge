package org.devlive.metaforge.spi.connection;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.spi.model.Response;

import java.sql.Connection;

@Slf4j
public class HttpConnection
        extends org.devlive.metaforge.spi.connection.Connection
{
    private HttpConfigure httpConfigure;
    private Response response;

    public HttpConnection(HttpConfigure httpConfigure, Response response)
    {
        super(httpConfigure, response);
    }

    public String formatJdbcUrl()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.httpConfigure.getProtocol());
        buffer.append("://");
        buffer.append(this.httpConfigure.getHost());
        buffer.append(":");
        buffer.append(this.httpConfigure.getPort());
        if (StringUtils.isNotEmpty(this.httpConfigure.getPath())) {
            buffer.append("/");
            buffer.append(this.httpConfigure.getPath());
        }
        return buffer.toString();
    }

    protected Connection openConnection()
    {
        try {
            this.httpConfigure = (HttpConfigure) getConfigure();
            this.response = getResponse();
            log.info("Connection protocol {}", this.httpConfigure.getProtocol());
            log.info("Connection url {}", formatJdbcUrl());
            response.setIsConnected(Boolean.TRUE);
        }
        catch (Exception ex) {
            log.error("Connection failed ", ex);
            response.setIsConnected(Boolean.FALSE);
            response.setMessage(ex.getMessage());
        }
        return null;
    }

    public void destroy()
    {
        log.info("Connection close skip");
    }
}
