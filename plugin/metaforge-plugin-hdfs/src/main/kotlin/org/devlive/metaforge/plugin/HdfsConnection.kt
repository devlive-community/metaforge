package org.devlive.metaforge.plugin

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.devlive.metaforge.spi.connection.Connection
import org.devlive.metaforge.spi.model.RuntimeConfigure
import org.devlive.metaforge.spi.model.Response
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import java.io.File

@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
class HdfsConnection : Connection
{
    var hdfsConfigure: Configuration? = null

    constructor(configure: RuntimeConfigure, response: Response) : super(configure, response)

    override fun openConnection(): java.sql.Connection?
    {
        try
        {
            this.hdfsConfigure = Configuration()
            this.hdfsConfigure !!.addResource(this.getPath("core-site.xml"))
            this.hdfsConfigure !!.addResource(this.getPath("hdfs-site.xml"))
            response?.isConnected = true
        }
        catch (ex: Exception)
        {
            response?.isConnected = false
            response?.message = ex.message
        }
        return null
    }

    override fun destroy()
    {
        this.hdfsConfigure = null
    }

    private fun getPath(file: String): Path
    {
        var path = mutableListOf(this.configure?.home, this.configure?.username?.get(), "Hdfs", file)
        if (StringUtils.isNotEmpty(this.configure?.id))
        {
            path = mutableListOf(this.configure?.home, this.configure?.username?.get(), "Hdfs", this.configure?.id, file)
        }
        return Path(path.joinToString(separator = File.separator))
    }
}
