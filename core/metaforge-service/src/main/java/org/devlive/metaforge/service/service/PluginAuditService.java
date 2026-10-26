package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.activity.HeatmapActivity;
import org.devlive.metaforge.service.entity.PluginAuditEntity;
import org.devlive.metaforge.service.itransient.ContributionRadar;
import org.devlive.metaforge.spi.model.Response;

import java.util.List;

public interface PluginAuditService
        extends BaseService<PluginAuditEntity>
{
    CommonResponse<Long> count();

    CommonResponse<List<HeatmapActivity>> getAllContribution();

    CommonResponse<List<ContributionRadar>> getContributionRadar();

    CommonResponse<PluginAuditEntity> getById(Long id);

    CommonResponse<Response> getData(String code);
}
