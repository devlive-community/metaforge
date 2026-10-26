package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.ChatEntity;
import org.devlive.metaforge.service.entity.MessageEntity;
import org.devlive.metaforge.service.repository.ChatRepository;
import org.devlive.metaforge.service.service.ChatService;
import org.devlive.metaforge.service.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping(value = "/api/v1/chat")
public class ChatController
        extends BaseController<ChatEntity>
{
    private final ChatRepository repository;
    private final ChatService service;
    private final MessageService messageService;

    public ChatController(ChatRepository repository, ChatService service, MessageService messageService)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
        this.messageService = messageService;
    }

    @Override
    public CommonResponse list(@RequestBody FilterBody filter)
    {
        return this.service.getAllByUser(filter);
    }

    @GetMapping(value = "{id}/messages")
    public CommonResponse<List<MessageEntity>> getMessagesByChat(@PathVariable(value = "id") Long id)
    {
        return this.messageService.getMessageByChatAndUser(id);
    }
}
