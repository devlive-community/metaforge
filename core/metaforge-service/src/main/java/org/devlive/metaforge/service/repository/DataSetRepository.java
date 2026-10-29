package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.SyncMode;
import org.devlive.metaforge.service.persistence.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DataSetRepository
        extends BaseRepository<DataSetEntity, Long>
{
    @Override
    default Visibility visibleTo(UserEntity user)
    {
        return Visibility.where("userId", user.getId());
    }

    Page<DataSetEntity> findAllByUserId(Long userId, Pageable pageable);

    List<DataSetEntity> findAllBySyncMode(SyncMode mode);
}
