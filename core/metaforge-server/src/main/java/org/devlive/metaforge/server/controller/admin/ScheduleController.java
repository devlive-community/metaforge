package org.devlive.metaforge.server.controller.admin;

import org.devlive.metaforge.common.response.CommonResponse;
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

@RestController
@RequestMapping(value = {"/api/v1/schedule", "/api/v1/admin/schedule"})
public class ScheduleController
{
    private final ScheduledRepository scheduledRepository;
    private final ScheduledService scheduledService;

    public ScheduleController(ScheduledRepository scheduledRepository, ScheduledService scheduledService)
    {
        this.scheduledRepository = scheduledRepository;
        this.scheduledService = scheduledService;
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
}
