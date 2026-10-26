package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface BaseRepository<T, ID>
        extends PagingAndSortingRepository<T, ID>
{
    Optional<T> findByCode(String code);

    void deleteByCode(String code);

    default Page<T> findAllByUser(UserEntity user, Pageable pageable)
    {
        throw new UnsupportedOperationException("findAllByUser not implemented");
    }
}
