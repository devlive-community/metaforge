package org.devlive.metaforge.service.service.impl;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.adapter.PageRequestAdapter;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.ScheduledEntity;
import org.devlive.metaforge.service.entity.ScheduledHistoryEntity;
import org.devlive.metaforge.service.repository.ScheduledHistoryRepository;
import org.devlive.metaforge.service.repository.ScheduledRepository;
import org.devlive.metaforge.service.service.ScheduledService;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduledServiceImpl
        implements ScheduledService
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ScheduledRepository scheduledRepository;
    private final ScheduledHistoryRepository historyRepository;

    public ScheduledServiceImpl(ScheduledRepository scheduledRepository, ScheduledHistoryRepository historyRepository)
    {
        this.scheduledRepository = scheduledRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    public CommonResponse<PageEntity<ScheduledHistoryEntity>> getHistory(Long id, FilterBody filter)
    {
        ScheduledEntity entity = ScheduledEntity.builder()
                .id(id)
                .build();
        Pageable pageable = PageRequestAdapter.of(filter);
        return CommonResponse.success(PageEntity.build(historyRepository.findAllByScheduledOrderByIdDesc(entity, pageable)));
    }

    @Override
    public CommonResponse<List<String>> getUpcomingTimes(Long id)
    {
        return scheduledRepository.findById(id)
                .map(task -> {
                    CommonResponse<List<String>> response;
                    try {
                        CronExpression cron = CronExpression.parse(task.getExpression());
                        ZonedDateTime next = ZonedDateTime.now();
                        List<String> times = new ArrayList<>();
                        for (int i = 0; i < 7; i++) {
                            next = cron.next(next);
                            if (next == null) {
                                break;
                            }
                            times.add(next.format(FORMATTER));
                        }
                        response = CommonResponse.success(times);
                    }
                    catch (IllegalArgumentException e) {
                        response = CommonResponse.failure(String.format("Invalid cron expression [ %s ]", task.getExpression()));
                    }
                    return response;
                })
                .orElseGet(() -> CommonResponse.failure(String.format("Scheduled task [ %s ] not found", id)));
    }
}
