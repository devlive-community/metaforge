package org.devlive.metaforge.plugin

import org.devlive.metaforge.spi.PluginService

class MatrixOneService : PluginService
{
    override fun driver(): String {
        return "com.mysql.cj.jdbc.Driver"
    }

    override fun connectType(): String {
        return "mysql"
    }
}
