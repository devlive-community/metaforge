package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.WorkflowEntity;
import org.devlive.metaforge.service.repository.WorkflowRepository;
import org.devlive.metaforge.service.service.WorkflowService;
import org.devlive.metaforge.service.workflow.WorkflowRun;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/workflow")
public class WorkflowController
        extends BaseController<WorkflowEntity>
{
    private final WorkflowService service;

    protected WorkflowController(WorkflowRepository repository, WorkflowService service)
    {
        super(repository, service);
        this.service = service;
    }

    @GetMapping(value = "/log/{code}")
    public CommonResponse<List<String>> log(@PathVariable String code)
    {
        return service.log(code);
    }

    @GetMapping(value = "/catalog")
    public CommonResponse<Map<String, Object>> catalog()
    {
        return service.catalog();
    }

    @GetMapping(value = "/statistics")
    public CommonResponse<Map<String, Long>> statistics()
    {
        return service.statistics();
    }

    @PostMapping(value = "/validate")
    public CommonResponse<List<String>> validate(@RequestBody WorkflowEntity workflow)
    {
        return service.validate(workflow);
    }

    @GetMapping(value = "/runs/{code}")
    public CommonResponse<List<WorkflowRun>> runs(@PathVariable String code)
    {
        return service.runs(code);
    }

    @GetMapping(value = "/run/{code}/{runId}")
    public CommonResponse<WorkflowRun> run(@PathVariable String code, @PathVariable String runId)
    {
        return service.run(code, runId);
    }

    @GetMapping(value = "/log/{code}/{runId}")
    public CommonResponse<List<String>> runLog(@PathVariable String code, @PathVariable String runId)
    {
        return service.log(code, runId);
    }

    @PutMapping(value = "stop/{code}")
    public CommonResponse<Boolean> stop(@PathVariable String code)
    {
        return service.stop(code);
    }

    @PutMapping(value = "restart/{code}")
    public CommonResponse<String> restart(@PathVariable String code)
    {
        return service.restart(code);
    }

    @PostMapping(value = "query-columns")
    public CommonResponse<List<Map<String, String>>> queryColumns(@RequestBody Map<String, String> body)
    {
        return service.queryColumns(body.get("source"), body.get("query"));
    }

    @PostMapping(value = "query-preview")
    public CommonResponse<Map<String, Object>> queryPreview(@RequestBody Map<String, String> body)
    {
        return service.queryPreview(body.get("source"), body.get("query"));
    }

    @PutMapping(value = "publish/{code}")
    public CommonResponse<WorkflowEntity> publish(@PathVariable String code)
    {
        return service.publish(code);
    }
}
