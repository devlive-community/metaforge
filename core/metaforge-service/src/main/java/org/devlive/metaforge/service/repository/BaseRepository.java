package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.persistence.Visibility;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface BaseRepository<T, ID>
        extends PagingAndSortingRepository<T, ID>
{
    Optional<T> findByCode(String code);

    /**
     * Rows of this aggregate the given user may list; see {@link org.devlive.metaforge.service.persistence.FilterQuery}.
     */
    default Visibility visibleTo(UserEntity user)
    {
        return Visibility.all();
    }
}
