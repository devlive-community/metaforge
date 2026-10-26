package org.devlive.metaforge.service.service.impl;

import org.devlive.metaforge.service.repository.admin.MenuRepository;
import org.devlive.metaforge.service.service.MenuService;
import org.springframework.stereotype.Service;

@Service
public class MenuServiceImpl
        implements MenuService
{
    private final MenuRepository menuRepository;

    public MenuServiceImpl(MenuRepository menuRepository)
    {
        this.menuRepository = menuRepository;
    }
}
