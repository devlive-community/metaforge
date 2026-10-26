package org.devlive.metaforge.scheduler

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.quartz.Job

@SuppressFBWarnings(value = ["EI_EXPOSE_REP2"])
data class SchedulerRequest(
    var name: String? = null,
    var group: String = "MetaForge-Scheduler-Group",
    var expression: String? = null,
    var jobId: String? = null,
    var job: Job? = null,
    private var _scheduler: org.quartz.Scheduler? = null,
    var createBeforeDelete: Boolean = false
)
{
    var scheduler: org.quartz.Scheduler?
        get() = _scheduler?.let { deepCopy(it) }
        set(value)
        {
            _scheduler = value?.let { deepCopy(it) }
        }

    private fun deepCopy(scheduler: org.quartz.Scheduler): org.quartz.Scheduler
    {
        return scheduler
    }

    companion object
    {
        @JvmStatic
        fun builder(): Builder
        {
            return Builder()
        }
    }

    class Builder
    {
        private var name: String? = null
        private var group: String = "MetaForge-Schedule-Group"
        private var expression: String? = null
        private var jobId: String? = null
        private var job: Job? = null
        private var scheduler: org.quartz.Scheduler? = null
        private var createBeforeDelete: Boolean = false

        fun name(name: String?) = apply { this.name = name }
        fun group(group: String) = apply { this.group = group }
        fun expression(expression: String?) = apply { this.expression = expression }
        fun jobId(jobId: String?) = apply { this.jobId = jobId }
        fun job(job: Job?) = apply { this.job = job }
        fun scheduler(scheduler: org.quartz.Scheduler?) = apply { this.scheduler = scheduler }
        fun createBeforeDelete(createBeforeDelete: Boolean) = apply { this.createBeforeDelete = createBeforeDelete }

        fun build() = SchedulerRequest(
            name = name,
            group = group,
            expression = expression,
            jobId = jobId,
            job = job,
            _scheduler = scheduler,
            createBeforeDelete = createBeforeDelete
        )
    }
}
