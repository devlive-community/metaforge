package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.exporter.ExportPlan;
import org.devlive.metaforge.service.service.DatabaseExportService;
import org.devlive.metaforge.service.service.TableExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Export of a database's tables: formats, samples, the plan check, the job and its file. */
@RestController
@RequestMapping(value = "/api/v1/export")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class DatabaseExportController
{
    private final DatabaseExportService service;

    public DatabaseExportController(DatabaseExportService service)
    {
        this.service = service;
    }

    @GetMapping(value = "formats")
    public CommonResponse<List<Map<String, Object>>> formats()
    {
        return this.service.formats();
    }

    @PostMapping(value = "{code}/{database}/sample")
    public CommonResponse<Map<String, Object>> sample(@PathVariable String code, @PathVariable String database, @RequestParam String table,
            @RequestParam(defaultValue = "3") int rows, @RequestBody ExportPlan plan)
    {
        return this.service.sample(code, database, plan, table, rows);
    }

    @PostMapping(value = "{code}/{database}/check")
    public CommonResponse<Map<String, Object>> check(@PathVariable String code, @PathVariable String database, @RequestBody ExportPlan plan)
    {
        return this.service.check(code, database, plan);
    }

    @PostMapping(value = "{code}/{database}/execute")
    public CommonResponse<Map<String, Object>> execute(@PathVariable String code, @PathVariable String database, @RequestBody ExportPlan plan)
    {
        return this.service.execute(code, database, plan);
    }

    @GetMapping(value = "jobs/{id}")
    public CommonResponse<Map<String, Object>> job(@PathVariable String id)
    {
        return this.service.job(id);
    }

    @PostMapping(value = "jobs/{id}/file")
    public ResponseEntity<?> file(@PathVariable String id)
    {
        CommonResponse<TableExportService.ExportFile> response = this.service.file(id);
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
