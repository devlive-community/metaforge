package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.service.entity.NotificationEntity;
import org.devlive.metaforge.service.repository.NotificationRepository;
import org.devlive.metaforge.service.service.NotificationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/notification")
public class NotificationController
        extends BaseController<NotificationEntity>
{
    private final NotificationService service;
    private final NotificationRepository repository;

    public NotificationController(NotificationService service, NotificationRepository repository)
    {
        super(repository, service);
        this.service = service;
        this.repository = repository;
    }
}
