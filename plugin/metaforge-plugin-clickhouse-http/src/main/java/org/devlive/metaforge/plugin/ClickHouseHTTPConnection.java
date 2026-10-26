package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.connection.HttpConfigure;
import org.devlive.metaforge.spi.connection.HttpConnection;
import org.devlive.metaforge.spi.model.Response;

public class ClickHouseHTTPConnection
        extends HttpConnection
{
    public ClickHouseHTTPConnection(HttpConfigure httpConfigure, Response response)
    {
        super(httpConfigure, response);
    }
}
