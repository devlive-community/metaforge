package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.DashboardShareBody;
import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.repository.DashboardRepository;
import org.devlive.metaforge.service.service.DashboardService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    /** Screens other users share with the signed-in user. */
    @GetMapping(value = "shared")
    public CommonResponse<List<DashboardEntity>> shared()
    {
        return service.shared();
    }

    /** Users and roles to share with. */
    @GetMapping(value = "share/candidates")
    public CommonResponse<List<DashboardShareBody.Member>> shareCandidates(@RequestParam(value = "keyword", required = false) String keyword)
    {
        return service.shareCandidates(keyword);
    }

    @GetMapping(value = "{code}/share")
    public CommonResponse<DashboardShareBody> getShare(@PathVariable String code)
    {
        return service.getShare(code);
    }

    @PutMapping(value = "{code}/share")
    public CommonResponse<DashboardShareBody> saveShare(@PathVariable String code, @RequestBody DashboardShareBody body)
    {
        return service.saveShare(code, body);
    }

    /** A dynamic chart of the screen, for anyone the screen is shared with. */
    @GetMapping(value = "{code}/reports/{report}/refresh")
    public ResponseEntity<CommonResponse<List<Map<String, Object>>>> refreshChart(@PathVariable String code, @PathVariable String report)
    {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.refreshChart(code, report));
    }
}
