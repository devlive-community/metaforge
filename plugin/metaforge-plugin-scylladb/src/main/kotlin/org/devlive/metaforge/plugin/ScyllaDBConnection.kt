package org.devlive.metaforge.plugin

import com.datastax.oss.driver.api.core.CqlSession
import org.devlive.metaforge.spi.connection.Connection
import org.devlive.metaforge.spi.model.Configure
import org.devlive.metaforge.spi.model.Response
import java.lang.Boolean
import java.net.InetSocketAddress
import kotlin.Exception

class ScyllaDBConnection(configure: Configure, response: Response) : Connection(configure, response)
{
    private var session: CqlSession? = null

    override fun openConnection(): java.sql.Connection?
    {
        try
        {
            this.session = CqlSession.builder()
                .addContactPoint(InetSocketAddress(configure?.host, configure !!.port))
                .withLocalDatacenter(configure.database.orElse("datacenter"))
                .build()
            response?.isConnected = Boolean.TRUE
        }
        catch (ex: Exception)
        {
            response?.isConnected = Boolean.FALSE
            response?.message = ex.message
        }
        return null
    }

    fun getSession(): CqlSession?
    {
        return session
    }

    override fun destroy()
    {
        this.session?.close()
    }
}
