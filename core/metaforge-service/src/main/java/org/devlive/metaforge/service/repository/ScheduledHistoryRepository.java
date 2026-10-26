package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.ScheduledEntity;
import org.devlive.metaforge.service.entity.ScheduledHistoryEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ScheduledHistoryRepository
        extends PagingAndSortingRepository<ScheduledHistoryEntity, Long>
{
    Page<ScheduledHistoryEntity> findAllByScheduledOrderByIdDesc(ScheduledEntity scheduled, Pageable pageable);

    Page<ScheduledHistoryEntity> findAllBySource(SourceEntity source, Pageable pageable);
}
