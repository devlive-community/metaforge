package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.UserLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserLogRepository
        extends PagingAndSortingRepository<UserLogEntity, Long>
{
    Page<UserLogEntity> findAllByUserOrderByCreateTimeDesc(UserEntity user, Pageable pageable);
}
