package org.devlive.metaforge.test.lib.http

import org.devlive.metaforge.lib.http.HttpClient
import org.devlive.metaforge.lib.http.HttpConfigure
import org.devlive.metaforge.lib.http.HttpMethod
import org.junit.Assert.assertNotNull
import org.junit.Test

class HttpClientTest
{
    private val configure: HttpConfigure = HttpConfigure()

    init
    {
        configure.autoConnected = java.lang.Boolean.FALSE
        configure.retry = 0
        configure.params = null
        configure.protocol = "https"
        configure.host = "metaforge.devlive.org"
        configure.port = 443
        configure.method = HttpMethod.GET
    }

    @Test
    fun test()
    {
        val client: HttpClient = HttpClient(this.configure)
        assertNotNull(client.execute())
    }
}
