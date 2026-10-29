package org.devlive.metaforge.service.persistence;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Hands the shared query helpers to the default methods of the service interfaces.
 */
@Component
@SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "The singleton publishes itself for the static service helpers")
public class Persistence
{
    private static volatile Persistence instance;

    private final FilterQuery filterQuery;
    private final References references;

    public Persistence(FilterQuery filterQuery, References references)
    {
        this.filterQuery = filterQuery;
        this.references = references;
        instance = this;
    }

    public static FilterQuery filterQuery()
    {
        return get().filterQuery;
    }

    public static References references()
    {
        return get().references;
    }

    /**
     * One page of a filtered listing, limited to the rows the user may see, with references loaded.
     */
    public static <T> Page<T> filtered(BaseRepository<T, ?> repository, FilterBody filter, UserEntity user)
    {
        return references().load(filterQuery().page(entityType(repository), filter, repository.visibleTo(user)));
    }

    /**
     * The aggregate type managed by a repository proxy.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> entityType(BaseRepository<T, ?> repository)
    {
        for (Class<?> candidate : repository.getClass().getInterfaces()) {
            Class<?> resolved = ResolvableType.forClass(candidate).as(BaseRepository.class).getGeneric(0).resolve();
            if (resolved != null) {
                return (Class<T>) resolved;
            }
        }
        throw new IllegalArgumentException("Cannot resolve the entity type of " + repository);
    }

    private static Persistence get()
    {
        if (instance == null) {
            throw new IllegalStateException("Persistence support is not initialized");
        }
        return instance;
    }
}
