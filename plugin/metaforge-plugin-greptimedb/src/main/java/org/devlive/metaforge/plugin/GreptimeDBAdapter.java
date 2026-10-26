package org.devlive.metaforge.plugin;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.plugin.response.GreptimeDBResponse;
import org.devlive.metaforge.plugin.response.record.Records;
import org.devlive.metaforge.spi.adapter.HttpAdapter;
import org.devlive.metaforge.spi.connection.HttpConfigure;
import org.devlive.metaforge.spi.connection.HttpConnection;
import org.devlive.metaforge.spi.connection.http.HttpClient;
import org.devlive.metaforge.spi.connection.http.HttpMethod;
import org.devlive.metaforge.spi.json.JSON;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.Time;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class GreptimeDBAdapter
        extends HttpAdapter
{
    public GreptimeDBAdapter(HttpConnection connection)
    {
        super(connection);
    }

    @Override
    public Response handlerExecute(String content)
    {
        Time processorTime = new Time();
        processorTime.setStart(new Date().getTime());
        Response response = this.connection.getResponse();
        HttpConfigure configure = new HttpConfigure();
        if (response.getIsConnected()) {
            List<String> headers = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Object> columns = new ArrayList<>();
            try {
                BeanUtils.copyProperties(configure, this.connection.getConfigure());
                configure.setAutoConnected(Boolean.FALSE);
                configure.setRetry(0);
                configure.setMethod(HttpMethod.POST);
                configure.setPath("v1/sql");
                Map<String, String> parameters = Maps.newHashMap();
                parameters.put("sql", content);
                configure.setParams(parameters);
                configure.setDecoded(true);
                HttpConnection httpConnection = new HttpConnection(configure, new Response());
                HttpClient httpClient = HttpClient.getInstance(configure, httpConnection);
                String body = httpClient.execute();
                GreptimeDBResponse requestResponse = JSON.objectmapper.readValue(body, GreptimeDBResponse.class);
                if (requestResponse.getCode() == 0 || ObjectUtils.isNotEmpty(requestResponse.getOutput())) {
                    response.setIsSuccessful(true);
                    Records records = requestResponse.getOutput().get(0).getRecords();
                    if (ObjectUtils.isNotEmpty(records.getSchema())) {
                        records.getSchema()
                                .getColumnSchemas()
                                .forEach(schema -> {
                                    headers.add(schema.getName());
                                    types.add(schema.getDataType());
                                });
                    }
                    records.getRows()
                            .forEach(record -> columns.add(record));
                }
                else {
                    response.setIsSuccessful(Boolean.FALSE);
                    response.setMessage(requestResponse.getError());
                }
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
