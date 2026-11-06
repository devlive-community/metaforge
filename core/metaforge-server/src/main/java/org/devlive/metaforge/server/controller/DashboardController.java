package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.repository.DashboardRepository;
import org.devlive.metaforge.service.service.DashboardService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/dashboard")
public class DashboardController
        extends BaseController<DashboardEntity>
{
    private final DashboardRepository repository;
    private final DashboardService service;

    protected DashboardController(DashboardRepository repository, DashboardService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }

    /** Opens a published data screen of the current user as their home page. */
    @PutMapping(value = "home/{code}")
    public CommonResponse<String> setHomePage(@PathVariable String code)
    {
        return service.setHomePage(code);
    }

    /** Restores the default home page. */
    @DeleteMapping(value = "home")
    public CommonResponse<String> clearHomePage()
    {
        return service.setHomePage(null);
    }
}
