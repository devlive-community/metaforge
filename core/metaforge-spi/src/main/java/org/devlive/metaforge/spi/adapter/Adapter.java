package org.devlive.metaforge.spi.adapter;

import com.google.common.base.Preconditions;
import org.devlive.metaforge.convert.ConvertFilter;
import org.devlive.metaforge.convert.model.ConvertRequest;
import org.devlive.metaforge.convert.model.ConvertResponse;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.spi.model.Response;

import java.util.List;

public interface Adapter
{
    Response handlerExecute(String content);

    default List<Object> handlerFormatter(PluginManager pluginManager, String format, List<String> headers, List<Object> columns)
    {
        return ConvertFilter.filter(pluginManager, format)
                .map(file -> {
                    ConvertRequest request = new ConvertRequest();
                    request.setHeaders(headers);
                    request.setColumns(columns);

                    ConvertResponse response = file.format(request);
                    Preconditions.checkArgument(Boolean.TRUE.equals(response.getSuccessful()), response.getMessage(), format);
                    return response.getColumns();
                })
                .orElseThrow(() -> new IllegalArgumentException("Unsupported format: " + format));
    }
}
