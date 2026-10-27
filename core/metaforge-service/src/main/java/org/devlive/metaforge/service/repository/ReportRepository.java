package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ReportRepository
        extends BaseRepository<ReportEntity, Long>
{
    @Override
    default Specification<ReportEntity> visibleTo(UserEntity user)
    {
        return (root, query, builder) -> builder.equal(root.get("user"), user);
    }

    Page<ReportEntity> findAllByUser(UserEntity user, Pageable pageable);
}
