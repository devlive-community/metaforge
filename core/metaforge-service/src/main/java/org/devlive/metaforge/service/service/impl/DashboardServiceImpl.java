package org.devlive.metaforge.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.entity.DashboardEntity;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.DashboardRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class DashboardServiceImpl
        implements DashboardService
{
    private final DashboardRepository repository;
    private final ReportRepository reportRepository;
    private final LinkRepository linkRepository;

    public DashboardServiceImpl(DashboardRepository repository, ReportRepository reportRepository, LinkRepository linkRepository)
    {
        this.repository = repository;
        this.reportRepository = reportRepository;
        this.linkRepository = linkRepository;
    }

    @Override
    @Transactional
    public CommonResponse<DashboardEntity> saveOrUpdate(BaseRepository<DashboardEntity, Long> baseRepository, DashboardEntity configure)
    {
        configure.setUser(UserDetailsService.getUser());
        List<Long> reportIds = new ArrayList<>();
        if (configure.getReports() != null) {
            configure.getReports()
                    .forEach(entity -> reportRepository.findByCode(entity.getCode())
                            .ifPresentOrElse(
                                    report -> {
                                        entity.setId(report.getId());
                                        reportIds.add(report.getId());
                                    },
                                    () -> {
                                        throw new RuntimeException("Report [ " + entity.getCode() + " ] not found");
                                    }
                            ));
        }
        DashboardEntity saved = repository.save(configure);
        linkRepository.replaceDashboardReports(saved.getId(), reportIds);
        return CommonResponse.success(saved);
    }

    @Override
    public CommonResponse<DashboardEntity> getById(BaseRepository<DashboardEntity, Long> baseRepository, Long id)
    {
        CommonResponse<DashboardEntity> response = DashboardService.super.getById(baseRepository, id);
        return withReports(response);
    }

    @Override
    public CommonResponse<DashboardEntity> getByCode(BaseRepository<DashboardEntity, Long> baseRepository, String code)
    {
        CommonResponse<DashboardEntity> response = DashboardService.super.getByCode(baseRepository, code);
        return withReports(response);
    }

    @Override
    @Transactional
    public CommonResponse<String> deleteByCode(BaseRepository<DashboardEntity, Long> baseRepository, String code)
    {
        repository.findByCode(code).ifPresent(dashboard -> linkRepository.deleteDashboard(dashboard.getId()));
        return DashboardService.super.deleteByCode(baseRepository, code);
    }

    private CommonResponse<DashboardEntity> withReports(CommonResponse<DashboardEntity> response)
    {
        DashboardEntity dashboard = response.getData();
        if (response.getStatus() && dashboard != null) {
            Set<ReportEntity> reports = new HashSet<>();
            reportRepository.findAllById(linkRepository.reportIdsOfDashboard(dashboard.getId())).forEach(reports::add);
            dashboard.setReports(reports);
        }
        return response;
    }
}
