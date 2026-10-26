package org.devlive.metaforge.scheduler

import org.devlive.metaforge.plugin.Service

interface SchedulerService : Service
{
    fun initialize(request: SchedulerRequest): SchedulerResponse

    fun stop(request: SchedulerRequest): SchedulerResponse
}
