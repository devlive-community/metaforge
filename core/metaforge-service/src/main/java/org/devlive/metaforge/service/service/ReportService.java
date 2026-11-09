package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.ReportEntity;

import java.util.List;
import java.util.Map;

public interface ReportService
        extends BaseService<ReportEntity>
{
    CommonResponse<PageEntity<ReportEntity>> getAll(FilterBody filter);

    CommonResponse<List<Map<String, Object>>> refreshChart(String code);
}
