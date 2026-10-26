package org.devlive.metaforge.server.configure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class SchedulerConfiguration
{
    @Bean
    public TaskScheduler taskScheduler()
    {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(4);
        taskScheduler.setRemoveOnCancelPolicy(true);
        taskScheduler.setThreadNamePrefix("MetaForge-TaskSchedulerThreadPool-");
        return taskScheduler;
    }
}
