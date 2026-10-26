package org.devlive.metaforge.server.controller;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.view.EntityView;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.SourceBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.PluginEntity;
import org.devlive.metaforge.service.entity.ScheduledHistoryEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.SourceService;
import org.devlive.metaforge.service.validation.ValidationGroup;
import org.devlive.metaforge.service.wrapper.ResponseWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController(value = "sourceControllerV2")
@RequestMapping(value = "/api/v2/source")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class SourceController
        extends BaseController<SourceEntity>
{
    private final SourceRepository repository;
    private final SourceService service;

    public SourceController(SourceRepository repository, SourceService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }

    @PostMapping(value = "test")
    @JsonView(value = {EntityView.UserView.class})
    public CommonResponse<ResponseWrapper> testConnection(@RequestBody @Validated(ValidationGroup.Crud.Create.class) SourceBody configure)
    {
        return this.service.testConnection(configure);
    }

    @PostMapping(value = "getHistory/{code}")
    public CommonResponse<PageEntity<ScheduledHistoryEntity>> getHistory(@PathVariable(value = "code") String code, @RequestBody FilterBody filter)
    {
        return this.service.getHistory(code, filter);
    }

    @GetMapping(value = "plugins")
    public CommonResponse<List<PluginEntity>> getPlugins()
    {
        return service.getPlugins();
    }
}
