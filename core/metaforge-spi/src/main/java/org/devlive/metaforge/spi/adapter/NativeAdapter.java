package org.devlive.metaforge.spi.adapter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.connection.Connection;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.parser.SqlParser;

@Slf4j
@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"},
        justification = "I prefer to suppress these FindBugs warnings")
public class NativeAdapter
        implements Adapter
{
    protected Connection connection;

    @Getter
    private SqlParser parser;

    public NativeAdapter(Connection connection)
    {
        this.connection = connection;
    }

    public NativeAdapter(Connection connection, SqlParser parser)
    {
        this.connection = connection;
        this.parser = parser;
    }

    @Override
    public Response handlerExecute(String content)
    {
        return null;
    }
}
