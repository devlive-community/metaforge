package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.service.DashboardService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Anonymous access to a data screen through its public link: the published screen limited to the
 * widgets the owner chose, and refreshes of its dynamic charts, which run as the owner.
 */
@RestController
@RequestMapping(value = "/api/v1/public/screen")
public class PublicScreenController
{
    private final DashboardService service;

    public PublicScreenController(DashboardService service)
    {
        this.service = service;
    }

    @GetMapping("{token}")
    public ResponseEntity<CommonResponse<Map<String, Object>>> view(@PathVariable("token") String token)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicView(token));
    }

    @GetMapping("{token}/reports/{report}/refresh")
    public ResponseEntity<CommonResponse<List<Map<String, Object>>>> refresh(@PathVariable("token") String token, @PathVariable("report") String report)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicRefresh(token, report));
    }
}
