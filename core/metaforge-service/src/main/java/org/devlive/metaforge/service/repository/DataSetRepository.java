package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.SyncMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface DataSetRepository
        extends BaseRepository<DataSetEntity, Long>
{
    @Override
    default Specification<DataSetEntity> visibleTo(UserEntity user)
    {
        return (root, query, builder) -> builder.equal(root.get("user"), user);
    }

    Page<DataSetEntity> findAllByUser(UserEntity user, Pageable pageable);

    Optional<DataSetEntity> findByCode(String code);

    @Query("SELECT d " +
            "FROM DataSetEntity d " +
            "WHERE d.syncMode = ?1")
    Set<DataSetEntity> findAllBySyncMode(SyncMode mode);
}
