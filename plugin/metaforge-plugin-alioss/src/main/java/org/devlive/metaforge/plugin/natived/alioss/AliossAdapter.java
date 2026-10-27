package org.devlive.metaforge.plugin.natived.alioss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectListing;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.adapter.NativeAdapter;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.devlive.metaforge.spi.model.Time;
import org.devlive.metaforge.sql.node.element.SelectElement;
import org.devlive.metaforge.sql.statement.SelectStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", "REC_CATCH_EXCEPTION"},
        justification = "I prefer to suppress these FindBugs warnings")
public class AliossAdapter
        extends NativeAdapter
{
    protected AliossConnection aliossConnection;
    private final AliossParser parser;

    public AliossAdapter(AliossConnection aliossConnection, AliossParser parser)
    {
        super(aliossConnection, parser);
        this.aliossConnection = aliossConnection;
        this.parser = parser;
    }

    @Override
    public Response handlerExecute(String content)
    {
        Time processorTime = new Time();
        processorTime.setStart(new Date().getTime());
        Response response = this.aliossConnection.getResponse();
        RuntimeConfigure configure = this.aliossConnection.getConfigure();
        if (response.getIsConnected()) {
            List<String> headers = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Object> columns = new ArrayList<>();
            try {
                SelectStatement statement = (SelectStatement) this.parser.getStatement();
                OSS client = this.aliossConnection.getOssClient();
                if (!statement.getSelectElements().isEmpty()) {
                    headers.addAll(statement.getSelectElements()
                            .stream()
                            .map(SelectElement::getColumn)
                            .collect(Collectors.toList()));
                }
                else {
                    headers.add("*");
                }
                types.add("String");
                ObjectListing objectListing = client.listObjects(configure.getDatabase().get(), statement.getFromSources().get(0).getTableName());
                objectListing.getObjectSummaries()
                        .forEach(column -> columns.add(Collections.singletonList(column.getKey())));
                response.setIsSuccessful(Boolean.TRUE);
            }
            catch (Exception ex) {
                log.error("Execute content failed content {} exception ", content, ex);
                response.setIsSuccessful(Boolean.FALSE);
                response.setMessage(ex.getMessage());
            }
            finally {
                response.setHeaders(headers);
                response.setTypes(types);
                response.setColumns(handlerFormatter(configure.getPluginManager(), configure.getFormat(), headers, columns));
            }
        }
        processorTime.setEnd(new Date().getTime());
        response.setProcessor(processorTime);
        return response;
    }
}
