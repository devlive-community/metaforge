package org.devlive.metaforge.plugin

import com.datastax.oss.driver.api.core.CqlSession
import org.devlive.metaforge.spi.connection.Connection
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.devlive.metaforge.spi.model.Response
import java.net.InetSocketAddress

class CassandraConnection(configure: RuntimeConfigure, response: Response) : Connection(configure, response)
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
            response.isConnected = true
        }
        catch (ex: Exception)
        {
            response.isConnected = false
            response.message = ex.message
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
