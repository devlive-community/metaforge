package org.devlive.metaforge.notify.dingtalk

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.common.utils.JsonUtils
import org.devlive.metaforge.common.utils.SignUtils
import org.devlive.metaforge.lib.http.HttpClient
import org.devlive.metaforge.lib.http.HttpConfigure
import org.devlive.metaforge.lib.http.HttpMethod
import org.devlive.metaforge.notify.dingtalk.model.ReturnModel
import org.devlive.metaforge.notify.model.NotifyRequest
import org.devlive.metaforge.notify.model.NotifyResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

// Kotlin's split and data class accessors read to SpotBugs as unchecked collection casts and exposed state.
@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION", "EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
object DingTalkUtils
{
    const val ACCESS_TOKEN = "accessToken"
    const val SECRET = "secret"

    private val log: Logger = getLogger(DingTalkUtils::class.java)

    /** The query parameters and JSON body of one robot call; never logged, they carry credentials. */
    @SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"], justification = "Built per call and read once")
    data class Call(val params: Map<String, String>, val body: String)

    /** Accepts the bare access_token or the robot's whole webhook address. */
    @JvmStatic
    fun accessToken(value: String?): String
    {
        val text = value?.trim().orEmpty()
        if (!text.startsWith("http://") && !text.startsWith("https://")) return text
        val query = runCatching { URI(text).rawQuery }.getOrNull().orEmpty()
        return query.split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it[0] == "access_token" && it.size == 2 }
            ?.let { URLDecoder.decode(it[1], StandardCharsets.UTF_8) }
            .orEmpty()
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun call(request: NotifyRequest): Call
    {
        val token = accessToken(request.configure[ACCESS_TOKEN] ?: request.access)
        require(token.isNotEmpty()) { "DingTalk robot access token is not configured" }
        val params = mutableMapOf("access_token" to token)
        val secret = (request.configure[SECRET] ?: request.secret)?.trim().orEmpty()
        if (secret.isNotEmpty())
        {
            val signed = SignUtils.sign(secret)
            params["sign"] = signed.sign
            params["timestamp"] = signed.timestamp.toString()
        }
        val title = request.title?.trim().orEmpty()
        val content = request.content.orEmpty()
        // A title reads better as markdown; plain messages stay plain text.
        val body = if (title.isEmpty())
        {
            mapOf("msgtype" to "text", "text" to mapOf("content" to content))
        }
        else
        {
            mapOf("msgtype" to "markdown", "markdown" to mapOf("title" to title, "text" to "#### $title\n\n$content"))
        }
        return Call(params, JsonUtils.toJSON(body))
    }

    @JvmStatic
    fun send(request: NotifyRequest): NotifyResponse
    {
        val response = NotifyResponse()
        val call = try
        {
            call(request)
        }
        catch (e: IllegalArgumentException)
        {
            response.successful = false
            response.message = e.message
            return response
        }
        val configure = HttpConfigure()
        configure.autoConnected = false
        configure.retry = 0
        configure.protocol = "https"
        configure.host = "oapi.dingtalk.com"
        configure.port = 443
        configure.path = "robot/send"
        configure.method = HttpMethod.POST
        configure.params = call.params
        configure.body = call.body
        val returned = runCatching { JsonUtils.toObject(HttpClient(configure).execute(), ReturnModel::class.java) }.getOrNull()
        response.successful = returned?.code?.toInt() == 0
        response.message = if (response.successful) null else returned?.message ?: "DingTalk did not answer"
        // Only the outcome is logged: the token and signature stay out of the logs.
        log.info("DingTalk notification {}", if (response.successful) "sent" else "failed: ${response.message}")
        return response
    }
}
