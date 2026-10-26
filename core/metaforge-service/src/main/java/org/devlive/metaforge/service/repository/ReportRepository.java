package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportRepository
        extends BaseRepository<ReportEntity, Long>
{
    Page<ReportEntity> findAllByUser(UserEntity user, Pageable pageable);
}
