package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.FunctionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FunctionsRepository
        extends BaseRepository<FunctionEntity, Long>
{
    Page<FunctionEntity> findAllByPluginContaining(Pageable pageable, String plugin);
}
