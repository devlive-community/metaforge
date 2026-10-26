package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DashboardRepository
        extends BaseRepository<DashboardEntity, Long>
{
    Page<DashboardEntity> findAllByUser(UserEntity user, Pageable pageable);
}
