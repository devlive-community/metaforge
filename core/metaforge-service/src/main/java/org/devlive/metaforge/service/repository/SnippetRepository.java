package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.SnippetEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface SnippetRepository
        extends BaseRepository<SnippetEntity, Long>
{
    @Override
    default Specification<SnippetEntity> visibleTo(UserEntity user)
    {
        return (root, query, builder) -> builder.equal(root.get("user"), user);
    }

    Page<SnippetEntity> findAllByUser(UserEntity user, Pageable pageable);
}
