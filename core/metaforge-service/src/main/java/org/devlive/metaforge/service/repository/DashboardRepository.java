package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.persistence.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DashboardRepository
        extends BaseRepository<DashboardEntity, Long>
{
    @Override
    default Visibility visibleTo(UserEntity user)
    {
        return Visibility.where("userId", user.getId());
    }

    Page<DashboardEntity> findAllByUserId(Long userId, Pageable pageable);
}
