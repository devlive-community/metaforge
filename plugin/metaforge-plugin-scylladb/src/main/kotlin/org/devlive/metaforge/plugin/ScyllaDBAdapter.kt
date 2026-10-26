package org.devlive.metaforge.plugin

import com.datastax.oss.driver.api.core.cql.ResultSet
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.adapter.Adapter
import org.devlive.metaforge.spi.model.Configure
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.Time
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.lang.Boolean
import java.util.*
import kotlin.Any
import kotlin.Exception
import kotlin.String
import kotlin.toString

@SuppressFBWarnings(value = ["RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"])
class ScyllaDBAdapter(private var connection: ScyllaDBConnection?) : Adapter
{
    private val log: Logger = getLogger(this.javaClass)

    override fun handlerExecute(content: String?): Response
    {
        val processorTime = Time()
        processorTime.start = Date().time
        val response: Response = this.connection !!.response
        val configure: Configure = this.connection !!.configure
        if (response.isConnected)
        {
            val headers: MutableList<String> = ArrayList()
            val types: MutableList<String> = ArrayList()
            val columns: MutableList<Any> = ArrayList()
            try
            {
                val resultSet: ResultSet = connection?.getSession() !!.execute(content !!)
                var isPresent = true
                resultSet.forEach { row ->
                    if (isPresent)
                    {
                        row.columnDefinitions.forEach {
                            types.add(it.type.asCql(true, true))
                            headers.add(it.name.asCql(true))
                        }
                        isPresent = false
                    }
                    val _columns: MutableList<Any> = ArrayList()
                    headers.forEach {
                        _columns.add(row.getObject(it).toString())
                    }
                    columns.add(_columns)
                }
                response.isSuccessful = Boolean.TRUE
            }
            catch (ex: Exception)
            {
                log.error("Execute content failed content {} exception ", content, ex)
                response.isSuccessful = Boolean.FALSE
                response.message = ex.message
            }
            finally
            {
                response.headers = headers
                response.types = types
                response.columns = handlerFormatter(configure.pluginManager, configure.format, headers, columns)
            }
        }
        processorTime.end = Date().time
        response.processor = processorTime
        return response
    }
}
