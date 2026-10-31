package org.devlive.metaforge.service.service;

import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.CodeUtils;
import org.devlive.metaforge.common.utils.NullAwareBeanUtils;
import org.devlive.metaforge.common.utils.ReflectionUtils;
import org.devlive.metaforge.service.SelfException;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.BaseEntity;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.security.UserDetailsService;

public interface BaseService<T extends BaseEntity>
{
    default CommonResponse<PageEntity<T>> getAll(BaseRepository<T, Long> repository, FilterBody filter)
    {
        try {
            return CommonResponse.success(PageEntity.build(Persistence.filtered(repository, filter, UserDetailsService.getUser())));
        }
        catch (Exception e) {
            return CommonResponse.failure("Failed to fetch data: " + e.getMessage());
        }
    }

    default CommonResponse<T> getById(BaseRepository<T, Long> repository, Long id)
    {
        return repository.findById(id)
                .map(Persistence.references()::load)
                .map(CommonResponse::success)
                .orElse(CommonResponse.failure(String.format("Resource [ %s ] not found", id)));
    }

    default CommonResponse<T> saveOrUpdate(BaseRepository<T, Long> repository, T configure)
    {
        if (configure.getId() != null) {
            repository.findById(configure.getId())
                    .ifPresent(value -> NullAwareBeanUtils.copyNullProperties(value, configure));
        }
        if (ReflectionUtils.hasField(configure, "user")) {
            ReflectionUtils.setFieldValue(configure, "user", UserDetailsService.getUser());
        }
        if (StringUtils.isEmpty(configure.getCode())) {
            configure.setCode(CodeUtils.generateCode(false));
        }
        return CommonResponse.success(repository.save(configure));
    }

    default CommonResponse<Long> deleteById(BaseRepository<T, Long> repository, Long id)
    {
        repository.deleteById(id);
        return CommonResponse.success(id);
    }

    default CommonResponse<String> deleteByCode(BaseRepository<T, Long> repository, String code)
    {
        return repository.findByCode(code)
                .map(entity -> {
                    repository.delete(entity);
                    return CommonResponse.success(code);
                })
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    default CommonResponse<T> getByCode(BaseRepository<T, Long> repository, String code)
    {
        return repository.findByCode(code)
                .map(Persistence.references()::load)
                .map(this::validatorUser)
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    /**
     * Toggle the active flag of a resource without touching its other columns.
     * Services may override to add guards (e.g. protected accounts).
     */
    default CommonResponse<T> changeActive(BaseRepository<T, Long> repository, String code, boolean active)
    {
        return repository.findByCode(code)
                .map(entity -> {
                    entity.setActive(active);
                    return CommonResponse.success(repository.save(entity));
                })
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    default <R> CommonResponse<R> validatorUser(R value)
    {
        if (!ownedBySelf(value)) {
            return CommonResponse.failure(ServiceState.USER_UNAUTHORIZED);
        }
        return CommonResponse.success(value);
    }

    default void isSelf(Object value)
    {
        if (!ownedBySelf(value)) {
            throw new SelfException(String.format("Resource [ %s ] not found", value.getClass().getName()));
        }
    }

    /**
     * Rows with an owner column belong to the signed-in user; rows without one are shared.
     */
    static boolean ownedBySelf(Object value)
    {
        if (!ReflectionUtils.hasField(value, "userId")) {
            return true;
        }
        Object owner = ReflectionUtils.getFieldValue(value, "userId");
        UserEntity loginUser = UserDetailsService.getUser();
        return owner == null || owner.equals(loginUser.getId());
    }
}
