package org.devlive.metaforge.notify.dingtalk

import org.devlive.metaforge.notify.model.NotifyRequest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DingTalkUtilsTest
{
    private val request: NotifyRequest = NotifyRequest()

    @Before
    fun before()
    {
        request.access = "ACCESS"
        request.content = "Test Message"
        request.secret = "SECRET"
    }

    @Test
    fun testSend()
    {
        Assert.assertFalse(
            DingTalkUtils.send(request)
                .successful
        )
    }
}
