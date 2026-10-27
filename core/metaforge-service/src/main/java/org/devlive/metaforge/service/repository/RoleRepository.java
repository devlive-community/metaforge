package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository
        extends BaseRepository<RoleEntity, Long>, JpaRepository<RoleEntity, Long>
{
    boolean existsByCode(String code);

    Optional<RoleEntity> findByName(String name);
}
