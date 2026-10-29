package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DataSetColumnRepository
        extends PagingAndSortingRepository<DataSetColumnEntity, Long>
{
    List<DataSetColumnEntity> findAllByDatasetId(Long datasetId);

    List<DataSetColumnEntity> findAllByDatasetIdOrderByPositionAsc(Long datasetId);

    @Modifying
    @Query("DELETE FROM metaforge_dataset_column WHERE dataset_id = :datasetId")
    void deleteAllByDatasetId(@Param("datasetId") Long datasetId);
}
