package org.devlive.metaforge.notify.model

import org.devlive.metaforge.notify.NotifyType

class NotifyRequest
{
    var title: String? = null
    var content: String? = null
    var sendTo: String? = null
    var access: String? = null
    var secret: String? = null
    var type: NotifyType = NotifyType.TEXT
}
