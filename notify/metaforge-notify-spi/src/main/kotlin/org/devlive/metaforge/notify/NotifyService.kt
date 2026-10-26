package org.devlive.metaforge.notify

import org.devlive.metaforge.notify.model.NotifyRequest
import org.devlive.metaforge.notify.model.NotifyResponse
import org.devlive.metaforge.plugin.Service

interface NotifyService : Service
{
    fun send(request: NotifyRequest): NotifyResponse
}
