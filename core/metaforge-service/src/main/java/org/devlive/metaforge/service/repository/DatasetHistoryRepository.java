package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DatasetHistoryRepository
        extends PagingAndSortingRepository<DatasetHistoryEntity, Long>
{
    Page<DatasetHistoryEntity> findAllByDatasetIdOrderByCreateTimeDesc(Long datasetId, Pageable pageable);

    /**
     * 启动恢复用：查出"未完结"的同步历史
     * Used by startup recovery to find sync history rows that did not reach a terminal state.
     */
    List<DatasetHistoryEntity> findAllByStateIn(Collection<RunState> states);

    @Modifying
    @Query("DELETE FROM metaforge_dataset_history WHERE dataset_id = :datasetId")
    void deleteAllByDatasetId(@Param("datasetId") Long datasetId);
}
