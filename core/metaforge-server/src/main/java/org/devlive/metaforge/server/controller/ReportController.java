package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.service.ReportService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController()
@RequestMapping(value = "/api/v1/report")
public class ReportController
        extends BaseController<ReportEntity>
{
    private final ReportService service;

    protected ReportController(ReportRepository repository, ReportService service)
    {
        super(repository, service);
        this.service = service;
    }

    @GetMapping("{code}/chart-refresh")
    public ResponseEntity<CommonResponse<List<Map<String, Object>>>> refreshChart(@PathVariable("code") String code)
    {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.refreshChart(code));
    }
}
