package org.devlive.metaforge.server.controller.user;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.service.SourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/source/admin")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class SourceAdminController
{
    private final SourceService sourceService;

    public SourceAdminController(SourceService sourceService)
    {
        this.sourceService = sourceService;
    }

    @GetMapping(value = "count")
    public CommonResponse<Long> count()
    {
        return this.sourceService.count();
    }
}
