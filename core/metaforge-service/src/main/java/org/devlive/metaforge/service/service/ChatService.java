package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.ChatEntity;
import org.devlive.metaforge.service.entity.PageEntity;

public interface ChatService
        extends BaseService<ChatEntity>
{
    CommonResponse<PageEntity<ChatEntity>> getAllByUser(FilterBody filter);
}
