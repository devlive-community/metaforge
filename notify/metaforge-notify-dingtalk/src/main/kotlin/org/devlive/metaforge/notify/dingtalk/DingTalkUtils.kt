package org.devlive.metaforge.notify.dingtalk

import org.devlive.metaforge.common.utils.JsonUtils
import org.devlive.metaforge.common.utils.SignUtils
import org.devlive.metaforge.lib.http.HttpClient
import org.devlive.metaforge.lib.http.HttpConfigure
import org.devlive.metaforge.lib.http.HttpMethod
import org.devlive.metaforge.notify.NotifyType
import org.devlive.metaforge.notify.dingtalk.model.ReturnModel
import org.devlive.metaforge.notify.dingtalk.model.TextModel
import org.devlive.metaforge.notify.model.NotifyRequest
import org.devlive.metaforge.notify.model.NotifyResponse
import org.apache.commons.lang3.StringUtils.isNotEmpty
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

object DingTalkUtils
{
    private val log: Logger = getLogger(DingTalkUtils::class.java)

    @JvmStatic
    fun send(request: NotifyRequest): NotifyResponse
    {
        val configure = HttpConfigure()
        configure.autoConnected = false
        configure.retry = 0
        configure.protocol = "https"
        configure.host = "oapi.dingtalk.com"
        configure.port = 443
        configure.path = "robot/send"
        configure.method = HttpMethod.POST

        val params = mutableMapOf("access_token" to request.access)
        if (isNotEmpty(request.secret))
        {
            val signResponse = SignUtils.sign(request.secret)
            log.info("Sign response: ${JsonUtils.toJSON(signResponse)}")
            params["sign"] = signResponse.sign
            params["timestamp"] = signResponse.timestamp.toString()
        }
        configure.params = params
        log.info("Notify request params: ${JsonUtils.toJSON(params)}")

        val text = TextModel()
        text.content = request.content
        configure.body = JsonUtils.toJSON(mapOf("text" to text, "msgtype" to formatMessageType(request)))
        log.info("Notify request body: ${configure.body}")

        val client = HttpClient(configure)
        val returnModel = JsonUtils.toObject(client.execute(), ReturnModel::class.java)
        val response = NotifyResponse()
        if (returnModel.code == 0)
        {
            response.successful = true
            response.message = null
        }
        else
        {
            response.successful = false
            response.message = returnModel.message
        }
        return response
    }

    private fun formatMessageType(request: NotifyRequest): String
    {
        if (request.type == NotifyType.TEXT)
        {
            return "text"
        }
        else
        {
            return "markdown"
        }
    }
}
