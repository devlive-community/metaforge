package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.service.MetadataService;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Capabilities of many sources in one request, for lists that decide per source what to offer. */
@RestController
@RequestMapping("/api/v1/source-management")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class SourceCapabilitiesController
{
    private final MetadataService service;

    public SourceCapabilitiesController(MetadataService service)
    {
        this.service = service;
    }

    @PostMapping("capabilities")
    public CommonResponse<Map<String, ManagementCapabilities>> capabilities(@RequestBody List<String> codes)
    {
        return service.getCapabilities(codes);
    }
}
