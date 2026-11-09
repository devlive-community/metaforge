package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.service.entity.UserOAuthEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface UserOAuthRepository
        extends PagingAndSortingRepository<UserOAuthEntity, Long>
{
    Optional<UserOAuthEntity> findByProviderAndSubject(String provider, String subject);
}
