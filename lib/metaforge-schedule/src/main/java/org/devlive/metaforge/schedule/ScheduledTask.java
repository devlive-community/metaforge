package org.devlive.metaforge.schedule;

import org.apache.commons.lang3.ObjectUtils;

import java.util.concurrent.ScheduledFuture;

public final class ScheduledTask
{
    volatile ScheduledFuture<?> future;

    /**
     * cancel the current task
     */
    public void cancel()
    {
        ScheduledFuture<?> future = this.future;
        if (ObjectUtils.isNotEmpty(future)) {
            future.cancel(true);
        }
    }
}
