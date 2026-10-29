package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.ExecuteDslBody;
import org.devlive.metaforge.service.entity.ExecuteEntity;

public interface ExecuteService
{
    CommonResponse<Object> execute(ExecuteEntity configure);

    CommonResponse<Object> execute(ExecuteDslBody configure);

    /**
     * Executes the query plan (EXPLAIN) of the given configuration against the
     * referenced source. Engines without a supported plan statement answer with
     * a failure response instead of being executed.
     *
     * @param configure the execution configuration carrying the source code and SQL
     * @return the plugin response holding the plan rows
     */
    CommonResponse<Object> plan(ExecuteEntity configure);
}
