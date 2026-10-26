package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.devlive.metaforge.service.entity.DataSetEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface DataSetColumnRepository
        extends PagingAndSortingRepository<DataSetColumnEntity, Long>
{
    List<DataSetColumnEntity> findAllByDataset(DataSetEntity dataset);

    List<DataSetColumnEntity> findAllByDatasetOrderByPositionAsc(DataSetEntity dataset);
}
