package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SourceRepository
        extends BaseRepository<SourceEntity, Long>
{
    List<SourceEntity> findAll();

    SourceEntity findByName(String name);

    Optional<SourceEntity> findByCode(String code);

    Page<SourceEntity> findAllByUserOrPublishIsTrue(UserEntity user, Pageable pageable);

    Long countByUserOrPublishIsTrue(UserEntity user);
}
