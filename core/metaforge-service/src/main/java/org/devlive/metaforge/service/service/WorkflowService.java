package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.WorkflowEntity;

import java.util.List;

public interface WorkflowService
        extends BaseService<WorkflowEntity>
{
    CommonResponse<Boolean> stop(String code);

    CommonResponse<List<String>> log(String code);

    CommonResponse<String> restart(String code);
}
