package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.adapter.FilterSpecification;
import org.devlive.metaforge.service.adapter.PageRequestAdapter;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface BaseRepository<T, ID>
        extends PagingAndSortingRepository<T, ID>, JpaSpecificationExecutor<T>
{
    Optional<T> findByCode(String code);

    void deleteByCode(String code);

    default Specification<T> visibleTo(UserEntity user)
    {
        return (root, query, builder) -> builder.conjunction();
    }

    default Page<T> findFiltered(FilterBody filter, UserEntity user)
    {
        return findAll(FilterSpecification.<T>of(filter).and(visibleTo(user)), PageRequestAdapter.of(filter));
    }

    default Page<T> findAllByUser(UserEntity user, Pageable pageable)
    {
        throw new UnsupportedOperationException("findAllByUser not implemented");
    }
}
