package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.NotificationEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface NotificationRepository
        extends BaseRepository<NotificationEntity, Long>
{
    @Modifying
    @Transactional
    @Query("UPDATE metaforge_notification SET entity_exists = :exists WHERE entity_code = :entityCode AND entity_type = :entityType")
    void updateEntityExistsByEntityCodeAndType(
            @Param("entityCode") String entityCode,
            @Param("entityType") EntityType entityType,
            @Param("exists") Boolean exists);

    Optional<NotificationEntity> findFirstByEntityCode(String entityCode);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("DELETE FROM metaforge_notification WHERE user_id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
