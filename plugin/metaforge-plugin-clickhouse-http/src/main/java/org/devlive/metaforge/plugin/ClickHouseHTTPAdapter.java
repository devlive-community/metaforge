package org.devlive.metaforge.plugin;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import org.apache.commons.beanutils.BeanUtils;
import org.devlive.metaforge.spi.adapter.HttpAdapter;
import org.devlive.metaforge.spi.connection.HttpConfigure;
import org.devlive.metaforge.spi.connection.HttpConnection;
import org.devlive.metaforge.spi.connection.http.HttpClient;
import org.devlive.metaforge.spi.connection.http.HttpMethod;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.Time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class ClickHouseHTTPAdapter
        extends HttpAdapter
{
    public ClickHouseHTTPAdapter(HttpConnection httpConnection)
    {
        super(httpConnection);
    }

    @Override
    public Response handlerExecute(String content)
    {
        Time processorTime = new Time();
        processorTime.setStart(new Date().getTime());
        Response response = this.connection.getResponse();
        HttpConfigure httpConfigure = new HttpConfigure();
        if (response.getIsConnected()) {
            List<String> headers = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Object> columns = new ArrayList<>();
            try {
                BeanUtils.copyProperties(httpConfigure, this.connection.getConfigure());
                httpConfigure.setAutoConnected(Boolean.FALSE);
                httpConfigure.setRetry(0);
                httpConfigure.setMethod(HttpMethod.POST);
                httpConfigure.setJsonBody(content + " FORMAT TabSeparatedWithNamesAndTypes");
                httpConfigure.setMediaType(MediaType.parse("text/plain; charset=utf-8"));
                HttpConnection httpConnection = new HttpConnection(httpConfigure, new Response());
                HttpClient httpClient = HttpClient.getInstance(httpConfigure, httpConnection);
                String responseBody = httpClient.execute();
                checkArgument(!responseBody.contains("DB::Exception"), responseBody);
                String[] data = responseBody.split("\n");
                for (int i = 0; i < data.length; i++) {
                    if (i == 0) {
                        headers.addAll(Arrays.asList(data[i].split("\t")));
                    }
                    else if (i == 1) {
                        types.addAll(Arrays.asList(data[i].split("\t")));
                    }
                    else {
                        columns.add(Arrays.asList(data[i].split("\t")));
                    }
                }
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
                response.setColumns(handlerFormatter(httpConfigure.getPluginManager(), httpConfigure.getFormat(), headers, columns));
            }
        }
        processorTime.setEnd(new Date().getTime());
        response.setProcessor(processorTime);
        return response;
    }
}
