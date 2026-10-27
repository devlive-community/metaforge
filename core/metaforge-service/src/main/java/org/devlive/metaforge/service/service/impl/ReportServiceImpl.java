package org.devlive.metaforge.service.service.impl;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.ReportEntity;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.repository.ReportRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.ReportService;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl
        implements ReportService
{
    private final ReportRepository repository;
    private final SourceRepository sourceRepository;
    private final DataSetRepository dataSetRepository;

    public ReportServiceImpl(ReportRepository repository, SourceRepository sourceRepository, DataSetRepository dataSetRepository)
    {
        this.repository = repository;
        this.sourceRepository = sourceRepository;
        this.dataSetRepository = dataSetRepository;
    }

    @Override
    public CommonResponse<PageEntity<ReportEntity>> getAll(FilterBody filter)
    {
        return CommonResponse.success(PageEntity.build(repository.findFiltered(filter, UserDetailsService.getUser())));
    }

    @Override
    public CommonResponse<ReportEntity> saveOrUpdate(BaseRepository<ReportEntity, Long> repository, ReportEntity configure)
    {
        if (configure.getSource() != null) {
            return sourceRepository.findByCode(configure.getSource().getCode())
                    .map(value -> {
                        configure.setUser(UserDetailsService.getUser());
                        configure.setSource(value);
                        return CommonResponse.success(repository.save(configure));
                    })
                    .orElseGet(() -> CommonResponse.failure(String.format("Source [ %s ] not found", configure.getSource().getCode())));
        }
        else {
            return dataSetRepository.findByCode(configure.getDataset().getCode())
                    .map(value -> {
                        configure.setUser(UserDetailsService.getUser());
                        configure.setDataset(value);
                        return CommonResponse.success(repository.save(configure));
                    })
                    .orElseGet(() -> CommonResponse.failure(String.format("Source [ %s ] not found", configure.getSource().getCode())));
        }
    }
}
