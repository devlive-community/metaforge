package org.devlive.metaforge.service.service.impl;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.adapter.PageRequestAdapter;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.ChatEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.ChatRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.ChatService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl
        implements ChatService
{
    private final ChatRepository repository;

    public ChatServiceImpl(ChatRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CommonResponse<PageEntity<ChatEntity>> getAllByUser(FilterBody filter)
    {
        Pageable pageable = PageRequestAdapter.of(filter);
        return CommonResponse.success(PageEntity.build(repository.findAllByUserId(UserDetailsService.getUser().getId(), pageable)));
    }

    @Override
    public CommonResponse<ChatEntity> saveOrUpdate(BaseRepository<ChatEntity, Long> repository, ChatEntity configure)
    {
        configure.setUser(UserDetailsService.getUser());
        return CommonResponse.success(repository.save(configure));
    }
}
