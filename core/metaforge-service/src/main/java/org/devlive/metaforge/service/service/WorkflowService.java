package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.WorkflowEntity;
import org.devlive.metaforge.service.workflow.WorkflowRun;

import java.util.List;
import java.util.Map;

public interface WorkflowService
        extends BaseService<WorkflowEntity>
{
    CommonResponse<Boolean> stop(String code);

    CommonResponse<List<String>> log(String code);

    CommonResponse<String> restart(String code);

    CommonResponse<Map<String, Object>> catalog();

    CommonResponse<List<String>> validate(WorkflowEntity workflow);

    CommonResponse<List<WorkflowRun>> runs(String code);

    CommonResponse<WorkflowRun> run(String code, String runId);

    CommonResponse<List<String>> log(String code, String runId);

    CommonResponse<Map<String, Long>> statistics();

    CommonResponse<WorkflowEntity> publish(String code);
}
