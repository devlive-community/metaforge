package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.DashboardEntity;

public interface DashboardService
        extends BaseService<DashboardEntity>
{
    /**
     * Opens one of the current user's published data screens as their home page, or restores the
     * default home page when {@code code} is null.
     */
    CommonResponse<String> setHomePage(String code);
}
