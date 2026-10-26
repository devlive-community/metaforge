package org.devlive.metaforge.schedule;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScheduledRunnable
        implements Runnable
{
    @Getter
    private final String name;

    public ScheduledRunnable(String name)
    {
        this.name = name;
    }

    @Override
    public void run()
    {
        log.info("Scheduled created");
    }
}
