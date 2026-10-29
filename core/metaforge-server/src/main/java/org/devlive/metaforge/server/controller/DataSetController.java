package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.enums.FunctionType;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.PluginMetadata;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.adhoc.Adhoc;
import org.devlive.metaforge.service.dataset.DatasetVirtualColumns;
import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.devlive.metaforge.service.entity.FunctionEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.service.DataSetService;
import org.devlive.metaforge.service.service.FunctionService;
import org.devlive.metaforge.spi.model.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController()
@RequestMapping(value = "/api/v1/dataset")
public class DataSetController
        extends BaseController<DataSetEntity>
{
    private final DataSetRepository repository;
    private final DataSetService service;

    private final FunctionService functions;
    private final InitializerConfigure initializer;

    protected DataSetController(DataSetRepository repository, DataSetService service, FunctionService functions, InitializerConfigure initializer)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
        this.functions = functions;
        this.initializer = initializer;
    }

    @PostMapping(value = "validateColumns")
    public CommonResponse<Boolean> validateColumns(@RequestBody List<DataSetColumnEntity> columns)
    {
        try {
            DatasetVirtualColumns.validate(columns);
            return CommonResponse.success(true);
        }
        catch (IllegalArgumentException exception) {
            return CommonResponse.failure(exception.getMessage());
        }
    }

    @GetMapping(value = "columnFunctions")
    public CommonResponse<Map<String, Object>> columnFunctions()
    {
        String engine = initializer.getDataSetConfigure().getType();
        if (engine == null || engine.isBlank()) {
            return CommonResponse.failure("Dataset storage engine is not configured");
        }
        CommonResponse<PageEntity<FunctionEntity>> response = functions.getAllByPlugin(engine);
        if (!response.getStatus()) {
            return CommonResponse.failure(String.valueOf(response.getMessage()));
        }
        List<Map<String, String>> available = response.getData().getContent().stream()
                .filter(function -> function.getType() == FunctionType.FUNCTION && function.getName() != null
                        && function.getName().matches("[a-zA-Z_][a-zA-Z0-9_]*") && !DatasetVirtualColumns.isAggregateFunction(function.getName()))
                .map(function -> Map.of("name", function.getName(),
                        "example", function.getExample() == null ? "" : function.getExample(),
                        "description", function.getDescription() == null ? "" : function.getDescription()))
                .collect(Collectors.toList());
        return CommonResponse.success(Map.of("engine", engine, "functions", available));
    }

    @GetMapping(value = "preview/{code}")
    public CommonResponse<Response> preview(@PathVariable String code)
    {
        return service.preview(code);
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
