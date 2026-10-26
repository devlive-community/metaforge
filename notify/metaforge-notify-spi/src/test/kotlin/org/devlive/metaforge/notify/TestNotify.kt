package org.devlive.metaforge.notify

import org.devlive.metaforge.notify.model.NotifyRequest
import org.devlive.metaforge.notify.model.NotifyResponse

class TestNotify : Notify
{
    override fun send(request: NotifyRequest): NotifyResponse
    {
        val response = NotifyResponse()
        return response
    }
}
