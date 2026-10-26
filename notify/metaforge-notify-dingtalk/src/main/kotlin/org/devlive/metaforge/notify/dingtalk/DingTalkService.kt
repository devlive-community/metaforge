package org.devlive.metaforge.notify.dingtalk

import org.devlive.metaforge.notify.NotifyService
import org.devlive.metaforge.notify.model.NotifyRequest
import org.devlive.metaforge.notify.model.NotifyResponse

class DingTalkService : NotifyService
{
    override fun send(request: NotifyRequest): NotifyResponse
    {
        return DingTalkUtils.send(request)
    }
}
