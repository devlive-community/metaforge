package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.ScheduledHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface ScheduledHistoryRepository
        extends PagingAndSortingRepository<ScheduledHistoryEntity, Long>
{
    Page<ScheduledHistoryEntity> findAllByScheduledIdOrderByIdDesc(Long scheduledId, Pageable pageable);

    Page<ScheduledHistoryEntity> findAllBySourceId(Long sourceId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM metaforge_scheduled_history WHERE source_id = :sourceId")
    void deleteAllBySourceId(@Param("sourceId") Long sourceId);

    @Modifying
    @Query("DELETE FROM metaforge_scheduled_history WHERE scheduled_id = :scheduledId")
    void deleteAllByScheduledId(@Param("scheduledId") Long scheduledId);
}
