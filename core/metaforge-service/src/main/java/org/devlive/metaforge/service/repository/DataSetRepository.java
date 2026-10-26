package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.SyncMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface DataSetRepository
        extends BaseRepository<DataSetEntity, Long>
{
    Page<DataSetEntity> findAllByUser(UserEntity user, Pageable pageable);

    Optional<DataSetEntity> findByCode(String code);

    @Query("SELECT d " +
            "FROM DataSetEntity d " +
            "WHERE d.syncMode = ?1")
    Set<DataSetEntity> findAllBySyncMode(SyncMode mode);
}
