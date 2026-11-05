package org.devlive.metaforge.notify.dingtalk

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.common.utils.JsonUtils
import org.devlive.metaforge.notify.NotifyFieldType
import org.devlive.metaforge.notify.model.NotifyRequest
import org.junit.Assert
import org.junit.Test

@SuppressFBWarnings(value = ["BC_BAD_CAST_TO_ABSTRACT_COLLECTION"], justification = "Kotlin collection helpers")
class DingTalkUtilsTest
{
    private fun request(configure: Map<String, String>, title: String? = null): NotifyRequest
    {
        val request = NotifyRequest()
        request.configure = configure
        request.title = title
        request.content = "Dataset sales was synced"
        return request
    }

    @Test
    fun theRobotTokenComesFromTheTokenOrTheWebhookAddress()
    {
        Assert.assertEquals("abc", DingTalkUtils.accessToken("abc"))
        Assert.assertEquals("abc", DingTalkUtils.accessToken(" https://oapi.dingtalk.com/robot/send?access_token=abc "))
        Assert.assertEquals("a+b", DingTalkUtils.accessToken("https://oapi.dingtalk.com/robot/send?x=1&access_token=a%2Bb"))
        Assert.assertEquals("", DingTalkUtils.accessToken("https://oapi.dingtalk.com/robot/send"))
    }

    @Test
    fun signedRobotsGetTheSignatureAndTitlesUseMarkdown()
    {
        val call = DingTalkUtils.call(request(mapOf(DingTalkUtils.ACCESS_TOKEN to "abc", DingTalkUtils.SECRET to "SECxyz"), "Sync finished"))
        Assert.assertEquals("abc", call.params["access_token"])
        Assert.assertNotNull(call.params["sign"])
        Assert.assertNotNull(call.params["timestamp"])
        val body = JsonUtils.objectmapper.readTree(call.body)
        Assert.assertEquals("markdown", body.path("msgtype").asText())
        Assert.assertEquals("Sync finished", body.path("markdown").path("title").asText())

        val plain = DingTalkUtils.call(request(mapOf(DingTalkUtils.ACCESS_TOKEN to "abc")))
        Assert.assertNull("unsigned robots get no signature", plain.params["sign"])
        Assert.assertEquals("text", JsonUtils.objectmapper.readTree(plain.body).path("msgtype").asText())
    }

    @Test
    fun aMissingTokenFailsWithoutCallingDingTalk()
    {
        val response = DingTalkUtils.send(request(emptyMap()))
        Assert.assertFalse(response.successful)
        Assert.assertEquals("DingTalk robot access token is not configured", response.message)
    }

    @Test
    fun theChannelDeclaresItsSettings()
    {
        val fields = DingTalkService().fields()
        Assert.assertEquals(listOf(DingTalkUtils.ACCESS_TOKEN, DingTalkUtils.SECRET), fields.map { it.name })
        Assert.assertTrue(fields[0].required)
        Assert.assertTrue(fields.all { it.type == NotifyFieldType.SECRET })
    }
}
