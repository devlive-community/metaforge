package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.SharedSourceBody;
import org.devlive.metaforge.service.body.SourceBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.PluginEntity;
import org.devlive.metaforge.service.entity.ScheduledHistoryEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.wrapper.ResponseWrapper;

import java.util.List;

public interface SourceService
        extends BaseService<SourceEntity>
{
    CommonResponse<Long> delete(Long id);

    CommonResponse<List<PluginEntity>> getPlugins();

    CommonResponse<Long> count();

    CommonResponse<Object> shared(SharedSourceBody configure);

    CommonResponse<ResponseWrapper> testConnection(SourceBody configure);

    CommonResponse<PageEntity<ScheduledHistoryEntity>> getHistory(String code, FilterBody filter);
}
