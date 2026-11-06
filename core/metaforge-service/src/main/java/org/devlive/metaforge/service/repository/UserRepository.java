package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository
        extends BaseRepository<UserEntity, Long>
{
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    UserEntity findByUsernameAndSystemTrue(String username);

    /** Only the home page column changes, so a concurrent profile save cannot be overwritten. */
    @Modifying
    @Query("UPDATE metaforge_user SET home_page = :homePage WHERE id = :id")
    void updateHomePage(@Param("id") Long id, @Param("homePage") String homePage);

    /** A deleted data screen stops being anyone's home page. */
    @Modifying
    @Query("UPDATE metaforge_user SET home_page = NULL WHERE home_page = :homePage")
    void clearHomePage(@Param("homePage") String homePage);
}
