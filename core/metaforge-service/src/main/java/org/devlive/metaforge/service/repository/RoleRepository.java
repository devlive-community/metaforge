package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.RoleEntity;

import java.util.Optional;

public interface RoleRepository
        extends BaseRepository<RoleEntity, Long>
{
    boolean existsByCode(String code);

    Optional<RoleEntity> findByName(String name);
}
