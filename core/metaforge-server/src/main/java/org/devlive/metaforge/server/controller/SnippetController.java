package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.service.entity.SnippetEntity;
import org.devlive.metaforge.service.repository.SnippetRepository;
import org.devlive.metaforge.service.service.SnippetService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/snippet")
public class SnippetController
        extends BaseController<SnippetEntity>
{
    private final SnippetRepository repository;
    private final SnippetService service;

    public SnippetController(SnippetRepository repository, SnippetService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }
}
