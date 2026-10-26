package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.ScheduledEntity;

import java.util.List;

public interface ScheduledRepository
        extends BaseRepository<ScheduledEntity, Long>
{
    List<ScheduledEntity> findAllByActiveIsTrueAndIsSystemIsTrue();
}
