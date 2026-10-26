package org.devlive.metaforge.spi.adapter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.connection.Connection;
import org.devlive.metaforge.spi.model.Response;

@Slf4j
@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"},
        justification = "I prefer to suppress these FindBugs warnings")
public class HttpAdapter
        implements Adapter
{
    protected Connection connection;

    public HttpAdapter(Connection connection)
    {
        this.connection = connection;
    }

    @Override
    public Response handlerExecute(String content)
    {
        return null;
    }
}
