package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.UserEntity;

import java.util.Optional;

public interface UserRepository
        extends BaseRepository<UserEntity, Long>
{
    Optional<UserEntity> findByUsername(String username);

    UserEntity findByUsernameAndSystemTrue(String username);
}
