package org.devlive.metaforge.plugin

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.adapter.Adapter
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.parser.SqlParser
import org.apache.commons.beanutils.BeanUtils
import org.apache.commons.lang3.ObjectUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

@SuppressFBWarnings(value = ["EI_EXPOSE_REP"])
class HdfsService : PluginService
{
    private val log: Logger = getLogger(HdfsService::class.java)

    private var configure: RuntimeConfigure? = null
    private var connection: HdfsConnection? = null
    private var response: Response? = null

    override fun validator(): String
    {
        return "SHOW DATABASES"
    }

    override fun connect(configure: RuntimeConfigure?)
    {
        try
        {
            this.response = Response()
            this.configure = RuntimeConfigure()
            BeanUtils.copyProperties(this.configure, configure)
            this.connection = HdfsConnection(this.configure !!, this.response !!)
        }
        catch (ex: Exception)
        {
            this.response?.isConnected = false
            this.response?.message = ex.message
        }
    }

    override fun execute(content: String?): Response
    {
        if (ObjectUtils.isNotEmpty(connection))
        {
            log.info("Execute hdfs plugin logic started")
            response = connection !!.response
            val processor: Adapter = HdfsAdapter(connection, SqlParser(content))
            response = processor.handlerExecute(content)
            log.info("Execute hdfs plugin logic end")
        }
        destroy()
        return response !!
    }

    override fun execute(configure: RuntimeConfigure?, content: String?): Response
    {
        connect(configure)
        return execute(content)
    }

    override fun destroy()
    {
        if (ObjectUtils.isNotEmpty(this.connection))
        {
            this.connection?.destroy()
            this.connection = null
        }
    }
}
