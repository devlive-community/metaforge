package org.devlive.metaforge.service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class ReportServiceImpl
        implements ReportService
{
    private static final Logger LOG = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final QueryChartRefresh chartRefresh;
    private final ReportRepository repository;
    private final SourceRepository sourceRepository;
    private final DataSetRepository dataSetRepository;
    private final LinkRepository linkRepository;

    public ReportServiceImpl(ReportRepository repository, SourceRepository sourceRepository, DataSetRepository dataSetRepository,
            LinkRepository linkRepository, QueryChartRefresh chartRefresh)
    {
        this.chartRefresh = chartRefresh;
        this.linkRepository = linkRepository;
        this.repository = repository;
        this.sourceRepository = sourceRepository;
        this.dataSetRepository = dataSetRepository;
    }

    @Override
    public CommonResponse<PageEntity<ReportEntity>> getAll(FilterBody filter)
    {
        return CommonResponse.success(PageEntity.build(Persistence.filtered(repository, filter, UserDetailsService.getUser())));
    }

    @Override
    public CommonResponse<ReportEntity> saveOrUpdate(BaseRepository<ReportEntity, Long> repository, ReportEntity configure)
    {
        var user = UserDetailsService.getUser();
        Optional<ReportEntity> existing = configure.getId() == null ? Optional.empty() : this.repository.findById(configure.getId())
                .filter(value -> Objects.equals(value.getUserId(), user.getId()));
        if (user.getId() == null || (configure.getId() != null && existing.isEmpty())) {
            return CommonResponse.failure("Report not found");
        }
        // Sharing changes only through the share endpoint: a save keeps the stored values, and a new report starts private.
        configure.setShareToken(existing.map(ReportEntity::getShareToken).orElse(null));
        configure.setShareEnabled(existing.map(ReportEntity::isShareEnabled).orElse(false));
        configure.setShareEmbed(existing.map(ReportEntity::isShareEmbed).orElse(false));
        configure.setShareOrigins(existing.map(ReportEntity::getShareOrigins).orElse(null));
        JsonNode chart = chartRefresh.chart(configure);
        if (chart != null && configure.getSource() == null) {
            return CommonResponse.failure("Dynamic chart data source not found");
        }
        if (configure.getSource() != null) {
            return sourceRepository.findByCode(configure.getSource().getCode())
                    .map(value -> {
                        if (chart != null) {
                            String error = chartRefresh.validationError(configure, value, user.getId(), chart);
                            if (error != null) {
                                return CommonResponse.<ReportEntity>failure(error);
                            }
                        }
                        configure.setUser(user);
                        configure.setSource(value);
                        return CommonResponse.success(repository.save(configure));
                    })
                    .orElseGet(() -> CommonResponse.failure(String.format("Source [ %s ] not found", configure.getSource().getCode())));
        }
        else if (configure.getDataset() != null) {
            return dataSetRepository.findByCode(configure.getDataset().getCode())
                    .map(value -> {
                        configure.setUser(user);
                        configure.setDataset(value);
                        return CommonResponse.success(repository.save(configure));
                    })
                    .orElseGet(() -> CommonResponse.failure(String.format("Source [ %s ] not found", configure.getSource().getCode())));
        }
        return CommonResponse.failure("Report data source not found");
    }

    @Override
    public CommonResponse<List<Map<String, Object>>> refreshChart(String code)
    {
        Long userId = UserDetailsService.getUser().getId();
        if (userId == null) {
            return CommonResponse.failure("Dynamic chart not found");
        }
        ReportEntity report = repository.findByCode(code)
                .filter(value -> value.isActive() && Objects.equals(value.getUserId(), userId))
                .orElse(null);
        if (report == null || report.getSourceId() == null) {
            return CommonResponse.failure("Dynamic chart not found");
        }
        JsonNode chart = chartRefresh.chart(report);
        SourceEntity source = sourceRepository.findById(report.getSourceId()).orElse(null);
        if (chart == null || source == null) {
            return CommonResponse.failure("Dynamic chart not found");
        }
        String error = chartRefresh.validationError(report, source, userId, chart);
        if (error != null) {
            return CommonResponse.failure(error);
        }
        try {
            return CommonResponse.success(chartRefresh.execute(report, source, chart));
        }
        catch (SQLException | RuntimeException e) {
            LOG.warn("Chart refresh failed for report {} ({})", report.getId(), e.getClass().getSimpleName());
            return CommonResponse.failure("Unable to refresh the chart. Check the data source and SQL, then try again.");
        }
    }

    @Override
    @Transactional
    public CommonResponse<String> deleteByCode(BaseRepository<ReportEntity, Long> repository, String code)
    {
        repository.findByCode(code).ifPresent(value -> linkRepository.deleteReport(value.getId()));
        return ReportService.super.deleteByCode(repository, code);
    }
}
