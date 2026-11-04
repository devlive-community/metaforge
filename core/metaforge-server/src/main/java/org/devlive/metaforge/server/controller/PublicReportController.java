package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.service.ReportShareService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Anonymous access to shared reports by their share token. Only the saved report definition is
 * evaluated; visitors cannot send SQL, queries or report codes.
 */
@RestController
@RequestMapping(value = "/api/v1/public/report")
public class PublicReportController
{
    private final ReportShareService service;

    public PublicReportController(ReportShareService service)
    {
        this.service = service;
    }

    @GetMapping("{token}")
    public ResponseEntity<CommonResponse<ReportShareService.PublicReport>> view(@PathVariable("token") String token)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.view(token));
    }

    @GetMapping("{token}/refresh")
    public ResponseEntity<CommonResponse<List<Map<String, Object>>>> refresh(@PathVariable("token") String token)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30))).body(service.refresh(token));
    }

    @GetMapping("{token}/data")
    public ResponseEntity<CommonResponse<Object>> data(@PathVariable("token") String token)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30))).body(service.data(token));
    }
}
