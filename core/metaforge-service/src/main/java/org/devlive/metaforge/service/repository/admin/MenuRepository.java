package org.devlive.metaforge.service.repository.admin;

import org.devlive.metaforge.common.enums.MenuEnum;
import org.devlive.metaforge.service.entity.MenuEntity;
import org.devlive.metaforge.service.repository.BaseRepository;

import java.util.List;

public interface MenuRepository
        extends BaseRepository<MenuEntity, Long>
{
    List<MenuEntity> findAllByType(MenuEnum type);
}
