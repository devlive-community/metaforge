package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.persistence.Visibility;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SourceRepository
        extends BaseRepository<SourceEntity, Long>
{
    @Override
    default Visibility visibleTo(UserEntity user)
    {
        return Visibility.where("userId", user.getId()).or("publish", true);
    }

    @Override
    List<SourceEntity> findAll();

    Optional<SourceEntity> findFirstByName(String name);

    List<SourceEntity> findAllByUserId(Long userId);

    @Query("SELECT COUNT(*) FROM metaforge_source WHERE user_id = :userId OR is_public = :published")
    Long countVisible(@Param("userId") Long userId, @Param("published") boolean published);
}
