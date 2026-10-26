package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.ReportEntity;

public interface ReportService
        extends BaseService<ReportEntity>
{
    CommonResponse<PageEntity<ReportEntity>> getAll(FilterBody filter);
}
