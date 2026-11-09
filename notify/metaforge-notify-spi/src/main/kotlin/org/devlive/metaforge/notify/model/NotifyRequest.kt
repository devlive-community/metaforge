package org.devlive.metaforge.notify.model

import org.devlive.metaforge.notify.NotifyType

class NotifyRequest
{
    var title: String? = null
    var content: String? = null
    var sendTo: String? = null

    /** The user's settings for this channel, keyed by [org.devlive.metaforge.notify.NotifyField.name]. */
    var configure: Map<String, String> = emptyMap()
        get() = field.toMap()
        set(value)
        {
            field = value.toMap()
        }

    @Deprecated("Channel settings travel in configure")
    var access: String? = null

    @Deprecated("Channel settings travel in configure")
    var secret: String? = null
    var type: NotifyType = NotifyType.TEXT
}
