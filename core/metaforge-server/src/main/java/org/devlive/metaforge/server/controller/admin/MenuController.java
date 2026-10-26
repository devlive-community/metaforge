package org.devlive.metaforge.server.controller.admin;

import org.devlive.metaforge.server.controller.BaseController;
import org.devlive.metaforge.service.entity.MenuEntity;
import org.devlive.metaforge.service.repository.admin.MenuRepository;
import org.devlive.metaforge.service.service.MenuService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/menu")
public class MenuController
        extends BaseController<MenuEntity>
{
    private final MenuRepository repository;
    private final MenuService service;

    public MenuController(MenuRepository repository, MenuService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }
}
