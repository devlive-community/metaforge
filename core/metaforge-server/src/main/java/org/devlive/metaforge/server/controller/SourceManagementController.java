package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.service.MetadataService;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementDownload;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Native management APIs do not share the variable database/table routes. */
@RestController
@RequestMapping("/api/v1/source-management/{code}")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class SourceManagementController
{
    private final MetadataService service;

    public SourceManagementController(MetadataService service)
    {
        this.service = service;
    }

    @GetMapping("capabilities")
    public CommonResponse<ManagementCapabilities> capabilities(@PathVariable String code)
    {
        return service.getCapabilities(code);
    }
    @PostMapping("namespaces")
    public CommonResponse<ManagementPage> namespaces(@PathVariable String code, @RequestBody ManagementRequest request)
    {
        return service.managementNamespaces(code, request);
    }

    @PostMapping("browse")
    public CommonResponse<ManagementPage> browse(@PathVariable String code, @RequestBody ManagementRequest request)
    {
        return service.managementList(code, request);
    }

    @PostMapping("detail")
    public CommonResponse<ManagementDetail> detail(@PathVariable String code, @RequestBody ManagementRequest request)
    {
        return service.managementDetail(code, request);
    }

    @PostMapping("download")
    public CommonResponse<ManagementDownload> download(@PathVariable String code, @RequestBody ManagementRequest request)
    {
        return service.managementDownload(code, request);
    }
}
