package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.common.NavigationPaths;
import org.devlive.metaforge.service.entity.MenuEntity;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.admin.MenuRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.service.MenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class MenuServiceImpl
        implements MenuService
{
    private final MenuRepository menuRepository;
    private final LinkRepository linkRepository;

    public MenuServiceImpl(MenuRepository menuRepository, LinkRepository linkRepository)
    {
        this.menuRepository = menuRepository;
        this.linkRepository = linkRepository;
    }

    @Override
    public CommonResponse<MenuEntity> saveOrUpdate(BaseRepository<MenuEntity, Long> repository, MenuEntity configure)
    {
        configure.setUrl(NavigationPaths.canonical(configure.getUrl()));
        return MenuService.super.saveOrUpdate(repository, configure);
    }

    @Override
    @Transactional
    public CommonResponse<String> deleteByCode(BaseRepository<MenuEntity, Long> repository, String code)
    {
        repository.findByCode(code).ifPresent(value -> linkRepository.deleteMenu(value.getId()));
        return MenuService.super.deleteByCode(repository, code);
    }
}
