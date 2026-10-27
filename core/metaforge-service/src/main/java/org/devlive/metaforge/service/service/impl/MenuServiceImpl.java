package org.devlive.metaforge.service.service.impl;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.common.NavigationPaths;
import org.devlive.metaforge.service.entity.MenuEntity;
import org.devlive.metaforge.service.repository.BaseRepository;
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

    @Override
    public CommonResponse<MenuEntity> saveOrUpdate(BaseRepository<MenuEntity, Long> repository, MenuEntity configure)
    {
        configure.setUrl(NavigationPaths.canonical(configure.getUrl()));
        return MenuService.super.saveOrUpdate(repository, configure);
    }
}
