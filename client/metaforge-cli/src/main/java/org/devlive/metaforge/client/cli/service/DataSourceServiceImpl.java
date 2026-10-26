package org.devlive.metaforge.client.cli.service;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.devlive.metaforge.client.cli.http.HttpCommon;
import org.devlive.metaforge.client.cli.http.HttpConfigure;
import org.devlive.metaforge.client.cli.http.HttpMethod;
import org.devlive.metaforge.client.cli.model.DataSource;
import org.devlive.metaforge.client.cli.response.ServerResponse;
import org.devlive.metaforge.client.cli.utils.TableUtils;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.toIntExact;

@Service
public class DataSourceServiceImpl
        implements DataSourceService
{
    private static final String url = "/api/v1/source";
    private final CacheService cacheService;

    public DataSourceServiceImpl(CacheService cacheService)
    {
        this.cacheService = cacheService;
    }

    private HttpConfigure getConfigure()
    {
        HttpConfigure configure = this.cacheService.getConfigure();
        return configure;
    }

    @Override
    public Table showList()
    {
        HttpConfigure configure = this.getConfigure();
        configure.setUrl(url);
        HttpCommon httpCommon = new HttpCommon(configure);
        ServerResponse serverResponse = httpCommon.withTokenForGet();
        LinkedTreeMap<String, Object> map = (LinkedTreeMap) serverResponse.getData();
        List<LinkedTreeMap<String, Object>> list = (List<LinkedTreeMap<String, Object>>) map.get("content");
        final List<DataSource> dataSources = new ArrayList<>();
        list.forEach(value -> {
            DataSource dataSource = new DataSource();
            // Double
            dataSource.setId(Integer.parseInt(String.valueOf(value.get("id")).split("\\.")[0]));
            dataSource.setName(String.valueOf(value.get("name")));
            dataSource.setType(String.valueOf(value.get("type")));
            dataSource.setDescription(String.valueOf(value.get("description")));
            dataSources.add(dataSource);
        });
        LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
        headers.put("id", "Id");
        headers.put("name", "Name");
        headers.put("type", "Type");
        headers.put("description", "Description");
        TableModel model = new BeanListTableModel<>(dataSources, headers);
        TableBuilder tableBuilder = new TableBuilder(model);
        TableUtils.applyStyle(tableBuilder);
        return tableBuilder.build();
    }

    @Override
    public Table getInfo()
    {
        HttpConfigure configure = this.getConfigure();
        configure.setUrl(url + "/" + configure.getSourceId());
        HttpCommon httpCommon = new HttpCommon(configure);
        ServerResponse serverResponse = httpCommon.withTokenForGet();
        LinkedTreeMap<String, Object> map = (LinkedTreeMap) serverResponse.getData();
        LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
        headers.put("id", "Id");
        headers.put("name", "Name");
        headers.put("type", "Type");
        headers.put("description", "Description");
        DataSource dataSource = new DataSource();
        dataSource.setId(toIntExact(configure.getSourceId()));
        dataSource.setName(String.valueOf(map.get("name")));
        dataSource.setType(String.valueOf(map.get("type")));
        dataSource.setDescription(String.valueOf(map.get("description")));
        TableModel model = new BeanListTableModel<>(Collections.singleton(dataSource), headers);
        TableBuilder tableBuilder = new TableBuilder(model);
        TableUtils.applyStyle(tableBuilder);
        return tableBuilder.build();
    }

    @Override
    public Table execute(String query)
    {
        HttpConfigure configure = this.getConfigure();
        configure.setUrl("/api/v1/execute");
        configure.setMethod(HttpMethod.POST);
        HttpCommon httpCommon = new HttpCommon(configure);
        Map<String, Object> stringObjectMap = new ConcurrentHashMap<>();
        stringObjectMap.put("content", query);
        stringObjectMap.put("format", "JSON");
        stringObjectMap.put("name", configure.getSourceId());
        ServerResponse serverResponse = httpCommon.withTokenForPost(new Gson().toJson(stringObjectMap));
        LinkedTreeMap<String, Object> map = (LinkedTreeMap) serverResponse.getData();

        LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
        List<LinkedTreeMap> columns = new ArrayList<>();
        List<String> responseHeaders = (List<String>) map.get("headers");
        List<LinkedTreeMap<String, Object>> responseColumns = (List<LinkedTreeMap<String, Object>>) map.get("columns");
        if (Boolean.valueOf(String.valueOf(map.get("isSuccessful")))) {
            responseHeaders.forEach(value -> headers.put(value, value));
            responseColumns.forEach(column -> columns.add(column));
        }

        TableModelBuilder<Object> builder = new TableModelBuilder<>();
        responseHeaders.forEach(header -> builder.addRow().addValue(header));
        responseColumns.forEach(column -> responseHeaders.forEach(header -> builder.addRow().addValue(column.get(header))));
        TableBuilder tableBuilder = new TableBuilder(builder.build());
        TableUtils.applyStyle(tableBuilder);
        return tableBuilder.build();
    }
}
