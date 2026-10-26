package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.UserQuestionBody;
import org.devlive.metaforge.service.entity.MessageEntity;

import java.util.List;

public interface MessageService
        extends BaseService<MessageEntity>
{
    CommonResponse<List<MessageEntity>> getMessageByChatAndUser(Long chatId);

    CommonResponse<MessageEntity> aiReply(UserQuestionBody configure);
}
