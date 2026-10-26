package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.SnippetEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SnippetRepository
        extends BaseRepository<SnippetEntity, Long>
{
    Page<SnippetEntity> findAllByUser(UserEntity user, Pageable pageable);
}
