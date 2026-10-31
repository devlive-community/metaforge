package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.ScheduledEntity;
import org.devlive.metaforge.service.entity.ScheduledHistoryEntity;

import java.util.List;

public interface ScheduledService
        extends BaseService<ScheduledEntity>
{
    CommonResponse<PageEntity<ScheduledHistoryEntity>> getHistory(Long id, FilterBody filter);

    CommonResponse<List<String>> getUpcomingTimes(Long id);

    /**
     * Toggle the built-in flag of a scheduled task from the admin table.
     */
    CommonResponse<ScheduledEntity> changeSystem(String code, boolean system);
}
