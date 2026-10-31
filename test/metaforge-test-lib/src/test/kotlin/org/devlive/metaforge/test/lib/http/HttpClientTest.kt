package org.devlive.metaforge.test.lib.http

import com.sun.net.httpserver.HttpServer
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.lib.http.HttpClient
import org.devlive.metaforge.lib.http.HttpConfigure
import org.devlive.metaforge.lib.http.HttpMethod
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

class HttpClientTest
{
    private val configure: HttpConfigure = HttpConfigure()
    private lateinit var server: HttpServer

    @Before
    @SuppressFBWarnings(value = ["NP_NULL_ON_SOME_PATH"], justification = "Kotlin lateinit server is initialized in @Before before every test")
    fun start()
    {
        // Serve locally so the test does not depend on an external site
        // being reachable (metaforge.devlive.org has been flaky).
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/") { exchange ->
            val bytes = "metaforge".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()

        configure.autoConnected = java.lang.Boolean.FALSE
        configure.retry = 0
        configure.params = null
        configure.protocol = "http"
        configure.host = "127.0.0.1"
        configure.port = server.address.port
        configure.method = HttpMethod.GET
    }

    @After
    @SuppressFBWarnings(value = ["NP_NULL_ON_SOME_PATH"], justification = "Kotlin lateinit server is initialized in @Before before every test")
    fun stop()
    {
        server.stop(0)
    }

    @Test
    fun test()
    {
        val client: HttpClient = HttpClient(this.configure)
        val body: String? = client.execute()
        assertNotNull(body)
        assertEquals("metaforge", body!!.trim())
    }
}
