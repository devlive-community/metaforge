package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.ExecuteDslBody;
import org.devlive.metaforge.service.entity.ExecuteEntity;

public interface ExecuteService
{
    CommonResponse<Object> execute(ExecuteEntity configure);

    CommonResponse<Object> execute(ExecuteDslBody configure);
}
