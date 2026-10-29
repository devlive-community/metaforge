package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.UserQuestionBody;
import org.devlive.metaforge.service.entity.MessageEntity;
import org.devlive.metaforge.service.repository.MessageRepository;
import org.devlive.metaforge.service.service.MessageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/message")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class MessageController
        extends BaseController<MessageEntity>
{
    private final MessageRepository repository;
    private final MessageService service;

    public MessageController(MessageRepository repository, MessageService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }

    @PostMapping(value = "ai/reply")
    public CommonResponse<MessageEntity> aiReply(@RequestBody UserQuestionBody configure)
    {
        return this.service.aiReply(configure);
    }
}
