package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.UserEntity;

import java.util.Optional;

public interface UserRepository
        extends BaseRepository<UserEntity, Long>
{
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    UserEntity findByUsernameAndSystemTrue(String username);
}
