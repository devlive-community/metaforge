package org.devlive.metaforge.plugin

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Configure
import org.devlive.metaforge.spi.model.Response
import org.apache.commons.beanutils.BeanUtils.copyProperties
import org.apache.commons.lang3.ObjectUtils.isNotEmpty
import org.slf4j.LoggerFactory.getLogger

@SuppressFBWarnings(value = ["EI_EXPOSE_REP"])
class CassandraService : PluginService
{
    private val log = getLogger(CassandraService::class.java)

    private var configure: Configure? = null
    private var connection: CassandraConnection? = null
    private var response: Response? = null
    override fun validator(): String
    {
        return "SELECT release_version AS version FROM system.local"
    }

    override fun connect(configure: Configure?)
    {
        try
        {
            log.info("Connecting to apache cassandra")
            response = Response()
            this.configure = Configure()
            copyProperties(this.configure, configure)
            connection = CassandraConnection(this.configure !!, response !!)
        }
        catch (ex: Exception)
        {
            response !!.isConnected = false
            response !!.message = ex.message
        }
    }

    override fun execute(content: String?): Response
    {
        if (isNotEmpty(connection))
        {
            log.info("Apache cassandra connection established")
            response = connection?.response
            val processor = CassandraAdapter(connection)
            response = processor.handlerExecute(content)
            log.info("Apache cassandra execution completed")
        }
        destroy()
        return response !!
    }

    override fun execute(configure: Configure?, content: String?): Response
    {
        connect(configure)
        return execute(content)
    }

    override fun destroy()
    {
        if (isNotEmpty(connection))
        {
            log.info("Apache cassandra driver destroyed")
            connection?.destroy()
            configure = null
        }
    }
}
