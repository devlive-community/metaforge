package org.devlive.metaforge.service.service.impl;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.adapter.PageRequestAdapter;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.ScheduledEntity;
import org.devlive.metaforge.service.entity.ScheduledHistoryEntity;
import org.devlive.metaforge.service.repository.ScheduledHistoryRepository;
import org.devlive.metaforge.service.service.ScheduledService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ScheduledServiceImpl
        implements ScheduledService
{
    private final ScheduledHistoryRepository historyRepository;

    public ScheduledServiceImpl(ScheduledHistoryRepository historyRepository)
    {
        this.historyRepository = historyRepository;
    }

    @Override
    public CommonResponse<PageEntity<ScheduledHistoryEntity>> getHistory(Long id, FilterBody filter)
    {
        ScheduledEntity entity = ScheduledEntity.builder()
                .id(id)
                .build();
        Pageable pageable = PageRequestAdapter.of(filter);
        return CommonResponse.success(PageEntity.build(historyRepository.findAllByScheduledOrderByIdDesc(entity, pageable)));
    }
}
