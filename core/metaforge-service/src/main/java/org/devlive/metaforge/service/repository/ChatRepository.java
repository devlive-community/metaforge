package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.ChatEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatRepository
        extends BaseRepository<ChatEntity, Long>
{
    Page<ChatEntity> findAllByUser(UserEntity user, Pageable pageable);
}
