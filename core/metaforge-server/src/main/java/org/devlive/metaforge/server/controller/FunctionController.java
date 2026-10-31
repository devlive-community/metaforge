package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FunctionUrlPreviewBody;
import org.devlive.metaforge.service.body.FunctionsImportBody;
import org.devlive.metaforge.service.entity.FunctionEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.repository.FunctionsRepository;
import org.devlive.metaforge.service.service.FunctionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = {"/api/v1/admin/function", "/api/v1/function"})
public class FunctionController
        extends BaseController<FunctionEntity>
{
    private final FunctionsRepository repository;
    private final FunctionService service;

    public FunctionController(FunctionsRepository repository, FunctionService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }

    @PutMapping(value = "import")
    public CommonResponse<Object> batchImport(@RequestBody @Validated FunctionsImportBody configure)
    {
        return this.service.batchImport(configure);
    }

    @PostMapping(value = "import/preview")
    public CommonResponse<String> previewUrl(@RequestBody @Validated FunctionUrlPreviewBody configure)
    {
        return this.service.previewUrl(configure);
    }

    @GetMapping(value = "list/{plugin}")
    public CommonResponse<PageEntity<FunctionEntity>> getAllByPlugin(@PathVariable(value = "plugin") String plugin)
    {
        return this.service.getAllByPlugin(plugin);
    }
}
