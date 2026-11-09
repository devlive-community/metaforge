package org.devlive.metaforge.notify.dingtalk

import org.devlive.metaforge.notify.NotifyField
import org.devlive.metaforge.notify.NotifyFieldType
import org.devlive.metaforge.notify.NotifyService
import org.devlive.metaforge.notify.model.NotifyRequest
import org.devlive.metaforge.notify.model.NotifyResponse

class DingTalkService : NotifyService
{
    override fun send(request: NotifyRequest): NotifyResponse
    {
        return DingTalkUtils.send(request)
    }

    override fun fields(): List<NotifyField>
    {
        return listOf(
            NotifyField(
                name = DingTalkUtils.ACCESS_TOKEN, label = "Robot access token", type = NotifyFieldType.SECRET, required = true,
                description = "The access_token of a DingTalk group robot, or its whole webhook address",
                placeholder = "https://oapi.dingtalk.com/robot/send?access_token=..."
            ),
            NotifyField(
                name = DingTalkUtils.SECRET, label = "Signing secret", type = NotifyFieldType.SECRET,
                description = "Needed when the robot's security setting is signing (SEC...)", placeholder = "SEC..."
            ),
        )
    }
}
