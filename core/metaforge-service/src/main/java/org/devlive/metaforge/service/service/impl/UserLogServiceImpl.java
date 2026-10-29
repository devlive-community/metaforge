package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.OrderBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.UserLogEntity;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.persistence.Visibility;
import org.devlive.metaforge.service.repository.UserLogRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.UserLogService;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
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
        if (filter.getOrders() == null || filter.getOrders().isEmpty()) {
            OrderBody newest = new OrderBody();
            newest.setColumn("createTime");
            newest.setOrder("desc");
            filter.setOrders(Collections.singletonList(newest));
        }
        UserEntity user = UserDetailsService.getUser();
        return CommonResponse.success(PageEntity.build(Persistence.filterQuery()
                .page(UserLogEntity.class, filter, Visibility.where("userId", user.getId()))));
    }
}
