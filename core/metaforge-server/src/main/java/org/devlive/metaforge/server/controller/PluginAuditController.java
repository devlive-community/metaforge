package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.PluginAuditEntity;
import org.devlive.metaforge.service.repository.PluginAuditRepository;
import org.devlive.metaforge.service.service.PluginAuditService;
import org.devlive.metaforge.spi.model.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/audit/plugin")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class PluginAuditController
        extends BaseController<PluginAuditEntity>
{
    private final PluginAuditRepository repository;
    private final PluginAuditService service;

    public PluginAuditController(PluginAuditRepository repository, PluginAuditService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }

    @GetMapping(value = "data/{code}")
    public CommonResponse<Response> getData(@PathVariable String code)
    {
        return service.getData(code);
    }
}
