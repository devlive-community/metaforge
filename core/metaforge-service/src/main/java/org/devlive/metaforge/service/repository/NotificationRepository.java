package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.NotificationEntity;
import org.devlive.metaforge.service.enums.EntityType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface NotificationRepository
        extends BaseRepository<NotificationEntity, Long>
{
    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.entityExists = :exists WHERE n.entityCode = :entityCode AND n.entityType = :entityType")
    void updateEntityExistsByEntityCodeAndType(
            @Param("entityCode") String entityCode,
            @Param("entityType") EntityType entityType,
            @Param("exists") Boolean exists);

    Optional<NotificationEntity> findFirstByEntityCode(String entityCode);
}
