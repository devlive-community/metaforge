package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.TableCompareBody;
import org.devlive.metaforge.service.service.TableCompareService;
import org.devlive.metaforge.service.service.TableExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Structure and data comparison of two tables of one source. */
@RestController
@RequestMapping(value = "/api/v1/metadata/{code}/compare")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class TableCompareController
{
    private final TableCompareService service;

    public TableCompareController(TableCompareService service)
    {
        this.service = service;
    }

    @PostMapping(value = "structure")
    public CommonResponse<Map<String, Object>> structure(@PathVariable String code, @RequestBody TableCompareBody configure)
    {
        return this.service.structure(code, configure);
    }

    @PostMapping(value = "data")
    public CommonResponse<Map<String, Object>> data(@PathVariable String code, @RequestBody TableCompareBody configure)
    {
        return this.service.data(code, configure);
    }

    /** Sync statements or a CSV report as a file, or the failure as the usual JSON response. */
    @PostMapping(value = "export")
    public ResponseEntity<?> export(@PathVariable String code, @RequestBody TableCompareBody configure)
    {
        CommonResponse<TableExportService.ExportFile> response = this.service.export(code, configure);
        if (!Boolean.TRUE.equals(response.getStatus()) || response.getData() == null) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response);
        }
        TableExportService.ExportFile file = response.getData();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.getName(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(file.getContent());
    }
}
