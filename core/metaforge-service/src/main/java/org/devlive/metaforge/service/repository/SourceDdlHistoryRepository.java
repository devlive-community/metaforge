package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.SourceDdlHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SourceDdlHistoryRepository
        extends PagingAndSortingRepository<SourceDdlHistoryEntity, Long>
{
    Page<SourceDdlHistoryEntity> findAllBySourceCodeAndDatabaseNameAndTableNameOrderByCreateTimeDescIdDesc(String sourceCode, String databaseName,
            String tableName, Pageable pageable);
}
