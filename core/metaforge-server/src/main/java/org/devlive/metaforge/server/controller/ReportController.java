package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.service.ReportService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/report")
public class ReportController
        extends BaseController<ReportEntity>
{
    private final ReportRepository repository;
    private final ReportService service;

    protected ReportController(ReportRepository repository, ReportService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }
}
