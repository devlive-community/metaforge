package org.devlive.metaforge.server.runner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.schedule.ScheduledCronRegistrar;
import org.devlive.metaforge.service.entity.ScheduledEntity;
import org.devlive.metaforge.service.enums.ScheduledType;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.devlive.metaforge.service.repository.ScheduledRepository;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.SourceService;
import org.devlive.metaforge.service.source.CheckScheduledRunnable;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class ScheduleRunner
        implements CommandLineRunner
{
    private final PluginManager pluginManager;
    private final ScheduledRepository scheduledRepository;
    private final SourceRepository sourceRepository;
    private final SourceService sourceService;
    private final ScheduledCronRegistrar scheduledCronRegistrar;
    private final ExecutorService executorService;
    private final Map<ScheduledType, Function<String, Runnable>> taskFactories;
    private final InstallationState installationState;

    public ScheduleRunner(PluginManager pluginManager,
            ScheduledRepository scheduledRepository,
            SourceRepository sourceRepository,
            SourceService sourceService,
            ScheduledCronRegistrar scheduledCronRegistrar,
            InstallationState installationState)
    {
        this.pluginManager = pluginManager;
        this.scheduledRepository = scheduledRepository;
        this.sourceRepository = sourceRepository;
        this.sourceService = sourceService;
        this.scheduledCronRegistrar = scheduledCronRegistrar;
        this.installationState = installationState;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "schedule-runner");
            thread.setDaemon(true);
            return thread;
        });
        this.taskFactories = initializeTaskFactories();
    }

    private Map<ScheduledType, Function<String, Runnable>> initializeTaskFactories()
    {
        Map<ScheduledType, Function<String, Runnable>> factories = new EnumMap<>(ScheduledType.class);

        factories.put(ScheduledType.SOURCE_CHECK,
                taskName -> new CheckScheduledRunnable(
                        taskName,
                        pluginManager,
                        sourceRepository
                )
        );

        return factories;
    }

    @Override
    public void run(String... args)
    {
        if (!installationState.isInstalled()) {
            log.info("Scheduled tasks skipped: instance is not installed");
            return;
        }
        try {
            scheduledRepository.findAllByActiveIsTrueAndIsSystemIsTrue()
                    .forEach(this::scheduleTask);
        }
        catch (Exception e) {
            log.error("Failed to initialize scheduled tasks", e);
        }
    }

    private void scheduleTask(ScheduledEntity task)
    {
        try {
            log.info("Adding new task [ {} ] to scheduler", task.getName());

            Function<String, Runnable> taskFactory = taskFactories.get(task.getType());
            if (taskFactory != null) {
                Runnable runnable = taskFactory.apply(task.getName());
                scheduledCronRegistrar.addCronTask(runnable, task.getExpression());
                executorService.submit(runnable);
            }
            else {
                log.warn("Unsupported task type [ {} ]", task.getType());
            }
        }
        catch (Exception e) {
            log.error("Failed to schedule task [ {} ]", task.getName(), e);
        }
    }

    @PreDestroy
    public void shutdown()
    {
        if (executorService != null && !executorService.isShutdown()) {
            try {
                log.info("Shutting down schedule runner executor service...");
                executorService.shutdown();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("Schedule runner executor service did not terminate in time, forcing shutdown...");
                    executorService.shutdownNow();
                }
            }
            catch (InterruptedException e) {
                log.warn("Schedule runner shutdown interrupted", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
