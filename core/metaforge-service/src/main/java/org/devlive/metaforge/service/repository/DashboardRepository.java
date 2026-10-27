package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface DashboardRepository
        extends BaseRepository<DashboardEntity, Long>
{
    @Override
    default Specification<DashboardEntity> visibleTo(UserEntity user)
    {
        return (root, query, builder) -> builder.equal(root.get("user"), user);
    }

    Page<DashboardEntity> findAllByUser(UserEntity user, Pageable pageable);
}
