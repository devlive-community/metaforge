package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.FunctionUrlPreviewBody;
import org.devlive.metaforge.service.body.FunctionsImportBody;
import org.devlive.metaforge.service.entity.FunctionEntity;
import org.devlive.metaforge.service.entity.PageEntity;

public interface FunctionService
        extends BaseService<FunctionEntity>
{
    CommonResponse<PageEntity<FunctionEntity>> getAllByFilter(FilterBody filter);

    CommonResponse<FunctionEntity> getById(Long id);

    CommonResponse<Object> batchImport(FunctionsImportBody configure);

    /** Fetch a remote function document so the wizard can preview it without CORS issues. */
    CommonResponse<String> previewUrl(FunctionUrlPreviewBody configure);

    CommonResponse<PageEntity<FunctionEntity>> getAllByPlugin(String plugin);
}
