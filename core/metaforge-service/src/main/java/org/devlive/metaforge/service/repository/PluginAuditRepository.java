package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.PluginAuditEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.persistence.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface PluginAuditRepository
        extends BaseRepository<PluginAuditEntity, Long>
{
    @Override
    default Visibility visibleTo(UserEntity user)
    {
        return Visibility.where("userId", user.getId());
    }

    Page<PluginAuditEntity> findAllByUserId(Long userId, Pageable pageable);

    Long countByUserId(Long userId);

    /** Audit rows of a user created at or after the given instant (activity charts are aggregated in Java). */
    List<PluginAuditEntity> findAllByUserIdAndCreateTimeGreaterThanEqual(Long userId, Date from);

    @Modifying
    @Query("DELETE FROM metaforge_source_query WHERE source_id = :sourceId")
    void deleteAllBySourceId(@Param("sourceId") Long sourceId);
}
