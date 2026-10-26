package org.devlive.metaforge.server.controller.user;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.activity.HeatmapActivity;
import org.devlive.metaforge.service.itransient.ContributionRadar;
import org.devlive.metaforge.service.service.PluginAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping(value = "/api/v1/audit/plugin/admin")
public class PluginAuditAdminController
{
    private final PluginAuditService pluginAuditService;

    public PluginAuditAdminController(PluginAuditService pluginAuditService)
    {
        this.pluginAuditService = pluginAuditService;
    }

    @GetMapping(value = "count")
    public CommonResponse<Long> count()
    {
        return this.pluginAuditService.count();
    }

    @GetMapping(value = "contribution")
    public CommonResponse<List<HeatmapActivity>> contribution()
    {
        return this.pluginAuditService.getAllContribution();
    }

    @GetMapping(value = "radar")
    public CommonResponse<List<ContributionRadar>> radar()
    {
        return this.pluginAuditService.getContributionRadar();
    }
}
