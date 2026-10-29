package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository
        extends BaseRepository<MessageEntity, Long>
{
    List<MessageEntity> findAllByChatIdOrderByCreateTimeAsc(Long chatId);

    List<MessageEntity> findAllByChatIdAndIdLessThanOrderByCreateTimeDesc(Long chatId, Long id, Pageable pageable);

    @Modifying
    @Query("DELETE FROM metaforge_message WHERE chat_id = :chatId")
    void deleteAllByChatId(@Param("chatId") Long chatId);
}
