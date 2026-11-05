package org.devlive.metaforge.notify

import org.devlive.metaforge.notify.model.NotifyRequest
import org.devlive.metaforge.notify.model.NotifyResponse
import org.devlive.metaforge.plugin.Service

interface NotifyService : Service
{
    /** Sends one message with the user's channel settings in [NotifyRequest.configure]. */
    fun send(request: NotifyRequest): NotifyResponse

    /** The settings each user fills in to use this channel; none by default. */
    fun fields(): List<NotifyField> = emptyList()
}
