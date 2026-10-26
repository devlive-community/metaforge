package org.devlive.metaforge.scheduler.local

import org.devlive.metaforge.scheduler.SchedulerRequest
import org.devlive.metaforge.scheduler.SchedulerResponse
import org.quartz.*
import org.slf4j.LoggerFactory

object QuartzEndpoint
{
    private val log = LoggerFactory.getLogger(this.javaClass)

    @JvmStatic
    fun createJob(request: SchedulerRequest): SchedulerResponse
    {
        val response = SchedulerResponse()
        val name = getJobName(request)
        val group = getJobGroup(request)
        try
        {
            if (request.createBeforeDelete)
            {
                val response = removeJob(request)
                if (! response.successful)
                {
                    throw IllegalArgumentException("Remove job failure " + response.message)
                }
            }

            val scheduler: Scheduler = request.scheduler ?: throw IllegalArgumentException("Scheduler must not null")

            val jobDetail: JobDetail = request.job?.let {
                JobBuilder.newJob(it::class.java)
                    .withIdentity(name, group)
                    .usingJobData("id", request.jobId)
                    .build()
            } ?: throw IllegalArgumentException("Job is null")

            val trigger: Trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-${request.name}", "trigger-group-${request.group}")
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(request.expression))
                .build()

            log.info("Add new job [ {} ] to group [ {} ]", name, group)
            scheduler.scheduleJob(jobDetail, trigger)

            if (! scheduler.isStarted)
            {
                log.info("Scheduler starting")
                scheduler.start()
            }
            response.successful = true
            log.info("Add new job [ {} ] to group [ {} ] successful", name, group)
        }
        catch (ex: Exception)
        {
            log.info("Add new job [ {} ] to group [ {} ] failure ", name, group, ex)
            response.successful = false
            response.message = ex.message
        }
        return response
    }

    @JvmStatic
    fun removeJob(request: SchedulerRequest): SchedulerResponse
    {
        val response = SchedulerResponse()
        val name = getJobName(request)
        val group = getJobGroup(request)
        try
        {
            val scheduler: Scheduler = request.scheduler ?: throw IllegalArgumentException("Scheduler must not null")

            log.info("Remove job [ {} ] from group [ {} ]", name, group)
            scheduler.deleteJob(JobKey(name, group))
            response.successful = true
            log.info("Remove job [ {} ] from group [ {} ] successful", name, group)
        }
        catch (ex: Exception)
        {
            log.info("Remove job [ {} ] from group [ {} ] failure ", name, group, ex)
            response.successful = false
            response.message = ex.message
        }
        return response
    }

    private fun getJobName(request: SchedulerRequest): String
    {
        return "job-${request.name}"
    }

    private fun getJobGroup(request: SchedulerRequest): String
    {
        return "job-group-${request.group}"
    }
}
