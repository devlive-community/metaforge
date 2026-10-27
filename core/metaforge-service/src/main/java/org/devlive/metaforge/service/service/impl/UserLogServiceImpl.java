package org.devlive.metaforge.service.service.impl;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.adapter.FilterSpecification;
import org.devlive.metaforge.service.adapter.PageRequestAdapter;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.UserLogEntity;
import org.devlive.metaforge.service.repository.UserLogRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.UserLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class UserLogServiceImpl
        implements UserLogService
{
    private final UserLogRepository userLogRepository;

    public UserLogServiceImpl(UserLogRepository userLogRepository)
    {
        this.userLogRepository = userLogRepository;
    }

    @Override
    public CommonResponse<PageEntity<UserLogEntity>> getAllByFilter(FilterBody filter)
    {
        Pageable pageable = PageRequestAdapter.of(filter);
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createTime"));
        }
        UserEntity user = UserDetailsService.getUser();
        return CommonResponse.success(PageEntity.build(this.userLogRepository.findAll(
                FilterSpecification.<UserLogEntity>of(filter).and((root, query, builder) -> builder.equal(root.get("user"), user)), pageable)));
    }
}
