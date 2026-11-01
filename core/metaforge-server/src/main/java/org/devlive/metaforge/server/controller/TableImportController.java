package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.importer.ImportPlan;
import org.devlive.metaforge.service.service.TableImportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** File import into a source's database: analyse the file, preview the plan, run it and follow the job. */
@RestController
@RequestMapping(value = "/api/v1/import")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class TableImportController
{
    private final TableImportService service;

    public TableImportController(TableImportService service)
    {
        this.service = service;
    }

    @GetMapping(value = "parsers")
    public CommonResponse<List<Map<String, Object>>> parsers()
    {
        return this.service.parsers();
    }

    @PostMapping(value = "{code}/{database}/analyze")
    public CommonResponse<Map<String, Object>> analyze(@PathVariable String code, @PathVariable String database, @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parser", required = false) String parser)
    {
        return this.service.analyze(code, database, file, parser);
    }

    @PostMapping(value = "{code}/{database}/preview")
    public CommonResponse<Map<String, Object>> preview(@PathVariable String code, @PathVariable String database, @RequestBody ImportPlan plan)
    {
        return this.service.preview(code, database, plan);
    }

    @PostMapping(value = "{code}/{database}/execute")
    public CommonResponse<Map<String, Object>> execute(@PathVariable String code, @PathVariable String database, @RequestBody ImportPlan plan)
    {
        return this.service.execute(code, database, plan);
    }

    @GetMapping(value = "jobs/{id}")
    public CommonResponse<Map<String, Object>> job(@PathVariable String id)
    {
        return this.service.job(id);
    }
}
