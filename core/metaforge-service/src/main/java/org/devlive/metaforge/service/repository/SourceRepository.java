package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface SourceRepository
        extends BaseRepository<SourceEntity, Long>
{
    @Override
    default Specification<SourceEntity> visibleTo(UserEntity user)
    {
        return (root, query, builder) -> builder.or(builder.equal(root.get("user"), user), builder.isTrue(root.get("publish")));
    }

    List<SourceEntity> findAll();

    SourceEntity findByName(String name);

    Optional<SourceEntity> findByCode(String code);

    Page<SourceEntity> findAllByUserOrPublishIsTrue(UserEntity user, Pageable pageable);

    Long countByUserOrPublishIsTrue(UserEntity user);
}
