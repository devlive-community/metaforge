package org.devlive.metaforge.scheduler.local

import org.devlive.metaforge.scheduler.SchedulerRequest
import org.devlive.metaforge.scheduler.SchedulerResponse
import org.devlive.metaforge.scheduler.SchedulerService

class LocalSchedulerService : SchedulerService
{
    override fun initialize(request: SchedulerRequest): SchedulerResponse
    {
        return QuartzEndpoint.createJob(request)
    }

    override fun stop(request: SchedulerRequest): SchedulerResponse
    {
        return QuartzEndpoint.removeJob(request)
    }
}
