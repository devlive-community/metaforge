package org.devlive.metaforge.server.configure;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;

@Component
public class AutowiringSpringBeanJobFactory
        extends SpringBeanJobFactory
        implements ApplicationContextAware
{
    private final transient ApplicationContext applicationContext;

    public AutowiringSpringBeanJobFactory(ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle)
            throws Exception
    {
        Object job = super.createJobInstance(bundle);
        applicationContext.getAutowireCapableBeanFactory()
                .autowireBean(job);
        return job;
    }
}
