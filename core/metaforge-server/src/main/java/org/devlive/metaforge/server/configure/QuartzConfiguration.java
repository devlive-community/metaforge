package org.devlive.metaforge.server.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class QuartzConfiguration
{
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(AutowiringSpringBeanJobFactory jobFactory)
    {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setAutoStartup(true);
        schedulerFactoryBean.setJobFactory(jobFactory);
        return schedulerFactoryBean;
    }
}
