package org.devlive.metaforge.server.controller.admin;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.server.runner.ScheduleRunner;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.ScheduledEntity;
import org.devlive.metaforge.service.entity.ScheduledHistoryEntity;
import org.devlive.metaforge.service.repository.ScheduledRepository;
import org.devlive.metaforge.service.service.ScheduledService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/schedule", "/api/v1/admin/schedule"})
public class ScheduleController
{
    private final ScheduledRepository scheduledRepository;
    private final ScheduledService scheduledService;
    private final ScheduleRunner scheduleRunner;

    public ScheduleController(ScheduledRepository scheduledRepository, ScheduledService scheduledService, ScheduleRunner scheduleRunner)
    {
        this.scheduledRepository = scheduledRepository;
        this.scheduledService = scheduledService;
        this.scheduleRunner = scheduleRunner;
    }

    @PostMapping(value = "list")
    public CommonResponse<PageEntity<ScheduledEntity>> getAllByFilter(@RequestBody FilterBody filter)
    {
        return scheduledService.getAll(scheduledRepository, filter);
    }

    @PostMapping(value = "{id}/history")
    public CommonResponse<PageEntity<ScheduledHistoryEntity>> getHistory(@PathVariable(value = "id") Long id,
            @RequestBody FilterBody filter)
    {
        return scheduledService.getHistory(id, filter);
    }

    @PostMapping(value = "{id}/upcoming")
    public CommonResponse<List<String>> getUpcomingTimes(@PathVariable(value = "id") Long id)
    {
        return scheduledService.getUpcomingTimes(id);
    }

    @PostMapping(value = "{id}/trigger")
    public CommonResponse<Boolean> trigger(@PathVariable(value = "id") Long id)
    {
        return scheduledRepository.findById(id)
                .map(task -> {
                    CommonResponse<Boolean> response;
                    if (scheduleRunner.triggerManually(task)) {
                        response = CommonResponse.success(true);
                    }
                    else {
                        response = CommonResponse.failure(String.format("Unsupported scheduled type [ %s ]", task.getType()));
                    }
                    return response;
                })
                .orElseGet(() -> CommonResponse.failure(String.format("Scheduled task [ %s ] not found", id)));
    }
}
