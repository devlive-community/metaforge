package org.devlive.metaforge.schedule;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScheduledCronRegistrar
        implements DisposableBean
{
    private final Map<Runnable, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>(16);

    private final TaskScheduler taskScheduler;

    public ScheduledCronRegistrar(TaskScheduler taskScheduler)
    {
        this.taskScheduler = taskScheduler;
    }

    public TaskScheduler getScheduler()
    {
        return this.taskScheduler;
    }

    public void addCronTask(Runnable task, String cronExpression)
    {
        addCronTask(new CronTask(task, cronExpression));
    }

    public void addCronTask(CronTask cronTask)
    {
        if (ObjectUtils.isNotEmpty(cronTask)) {
            Runnable task = cronTask.getRunnable();
            if (this.scheduledTasks.containsKey(task)) {
                removeCronTask(task);
            }
            this.scheduledTasks.put(task, scheduleCronTask(cronTask));
        }
    }

    public void removeCronTask(Runnable task)
    {
        ScheduledRunnable sourceScheduledRunnable = (ScheduledRunnable) task;
        Optional<Map.Entry<Runnable, ScheduledTask>> findScheduledRunnable = this.scheduledTasks.entrySet()
                .stream()
                .filter(v -> {
                    ScheduledRunnable targetScheduledRunnable = (ScheduledRunnable) v.getKey();
                    return targetScheduledRunnable.getName().equals(sourceScheduledRunnable.getName());
                })
                .findFirst();
        if (findScheduledRunnable.isPresent()) {
            findScheduledRunnable.get().getValue().cancel();
            this.scheduledTasks.remove(findScheduledRunnable.get().getKey());
        }
    }

    public ScheduledTask scheduleCronTask(CronTask cronTask)
    {
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.future = this.taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
        return scheduledTask;
    }

    @Override
    public void destroy()
    {
        for (ScheduledTask task : this.scheduledTasks.values()) {
            task.cancel();
        }
        this.scheduledTasks.clear();
    }
}
