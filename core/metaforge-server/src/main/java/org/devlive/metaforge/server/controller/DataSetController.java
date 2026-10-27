package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.adhoc.Adhoc;
import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.service.DataSetService;
import org.devlive.metaforge.spi.model.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController()
@RequestMapping(value = "/api/v1/dataset")
public class DataSetController
        extends BaseController<DataSetEntity>
{
    private final DataSetRepository repository;
    private final DataSetService service;

    protected DataSetController(DataSetRepository repository, DataSetService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }

    @PutMapping(value = "rebuild/{code}")
    public CommonResponse rebuild(@PathVariable String code)
    {
        return service.rebuild(code);
    }

    @GetMapping(value = "columns/{code}")
    public CommonResponse<List<DataSetColumnEntity>> getColumnsByCode(@PathVariable String code)
    {
        return service.getColumnsByCode(code);
    }

    @PutMapping(value = "syncData/{code}")
    public CommonResponse<DataSetEntity> syncData(@PathVariable String code,
            @RequestBody(required = false) java.util.Map<String, String> overrides)
    {
        return service.syncData(code, overrides);
    }

    @GetMapping(value = "syncFields/{code}")
    public CommonResponse<java.util.List<org.devlive.metaforge.plugin.configure.RuntimeConfigureField>> syncFields(@PathVariable String code)
    {
        return service.getSyncFields(code);
    }

    @PutMapping(value = "clearData/{code}")
    public CommonResponse<Boolean> clearData(@PathVariable String code)
    {
        return service.clearData(code);
    }

    @PostMapping(value = "adhoc/{code}")
    public CommonResponse<Response> adhoc(@PathVariable String code,
            @RequestBody Adhoc configure)
    {
        return service.adhoc(code, configure);
    }

    @PostMapping(value = "history/{code}")
    public CommonResponse<PageEntity<DatasetHistoryEntity>> history(@PathVariable String code,
            @RequestBody FilterBody filter)
    {
        return this.service.getHistory(code, filter);
    }

    @GetMapping(value = "history/log/{id}")
    public CommonResponse<List<String>> historyLog(@PathVariable Long id)
    {
        return this.service.getHistoryLog(id);
    }

    @PutMapping(value = "history/stop/{id}")
    public CommonResponse<Boolean> historyStop(@PathVariable Long id)
    {
        return this.service.stopHistory(id);
    }

    @GetMapping(value = "getActuators")
    public CommonResponse<Set<PluginMetadata>> getActuators()
    {
        return service.getActuators();
    }
}
