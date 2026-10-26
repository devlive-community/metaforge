package org.devlive.metaforge.scheduler.local

import org.devlive.metaforge.scheduler.SchedulerRequest
import org.junit.Before
import org.junit.Test
import org.quartz.impl.StdSchedulerFactory
import kotlin.test.assertTrue

class QuartzEndpointTest
{
    private val name: String = "TestJob"
    private val group: String = "TestGroup"
    private val request = SchedulerRequest()

    @Before
    fun before()
    {
        request.name = name
        request.group = group
        request.expression = "*/10 * * * * ?"
        request.job = ExampleJob()
        request.scheduler = StdSchedulerFactory.getDefaultScheduler()
    }

    @Test
    fun testCreateJob()
    {
        assertTrue(QuartzEndpoint.createJob(request).successful)
    }

    @Test
    fun testRemoveJob()
    {
        QuartzEndpoint.createJob(request)
        assertTrue(QuartzEndpoint.removeJob(request).successful)
    }
}
