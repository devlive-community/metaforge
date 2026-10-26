package org.devlive.metaforge.server.runner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.scheduler.SchedulerRequest;
import org.devlive.metaforge.scheduler.SchedulerService;
import org.devlive.metaforge.service.enums.SyncMode;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.devlive.metaforge.service.initializer.job.DatasetJob;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.quartz.Scheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SchedulerRunner
        implements CommandLineRunner
{
    private final PluginManager pluginManager;
    private final DataSetRepository repository;
    private final Scheduler scheduler;
    private final InstallationState installationState;

    public SchedulerRunner(PluginManager pluginManager, DataSetRepository repository, Scheduler scheduler, InstallationState installationState)
    {
        this.pluginManager = pluginManager;
        this.repository = repository;
        this.scheduler = scheduler;
        this.installationState = installationState;
    }

    @Override
    public void run(String... args)
    {
        if (!installationState.isInstalled()) {
            log.info("Scheduler initializer skipped: instance is not installed");
            return;
        }
        log.info("Start scheduler initializer");
        try {
            repository.findAllBySyncMode(SyncMode.TIMING)
                    .forEach(item -> {
                        log.info("Dataset [ {} ] will be scheduled", item.getName());
                        pluginManager.getPlugin(item.getScheduler())
                            .ifPresentOrElse(
                                    plugin -> {
                                        SchedulerRequest request = SchedulerRequest.builder()
                                                .name(item.getCode())
                                                .group("metaforge")
                                                .expression(item.getExpression())
                                                .jobId(item.getCode())
                                                .createBeforeDelete(true)
                                                .build();

                                        SchedulerService schedulerService = plugin.getService(SchedulerService.class);
                                        if (item.getScheduler().equalsIgnoreCase("LocalScheduler")) {
                                            request.setJob(new DatasetJob());
                                            request.setScheduler(this.scheduler);
                                        }
                                        schedulerService.initialize(request);
                                    },
                                    () -> log.error("Scheduler [ {} ] not found", item.getScheduler())
                            );
                });
        }
        catch (Exception e) {
            // 未安装的空库：等待 setup 向导完成并重启后再次调度
            log.warn("Scheduler initializer skipped, dataset tables missing (instance not installed?): {}", e.getMessage());
        }
        log.info("End scheduler initializer");
    }
}
