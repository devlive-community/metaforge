package org.devlive.metaforge.service.service.impl;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.DashboardRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.DashboardService;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl
        implements DashboardService
{
    private final DashboardRepository repository;
    private final ReportRepository reportRepository;

    public DashboardServiceImpl(DashboardRepository repository, ReportRepository reportRepository)
    {
        this.repository = repository;
        this.reportRepository = reportRepository;
    }

    @Override
    public CommonResponse<DashboardEntity> saveOrUpdate(BaseRepository<DashboardEntity, Long> baseRepository, DashboardEntity configure)
    {
        configure.setUser(UserDetailsService.getUser());
        configure.getReports()
                .forEach(entity -> reportRepository.findByCode(entity.getCode())
                        .ifPresentOrElse(
                                report -> entity.setId(report.getId()),
                                () -> {
                                    throw new RuntimeException("Report [ " + entity.getCode() + " ] not found");
                                }
                        ));
        return CommonResponse.success(repository.save(configure));
    }
}
