package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.UserLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface UserLogRepository
        extends PagingAndSortingRepository<UserLogEntity, Long>
{
    Page<UserLogEntity> findAllByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM metaforge_user_log WHERE user_id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
