package org.devlive.metaforge.plugin.dolphindb

import org.devlive.metaforge.spi.PluginService
import org.devlive.metaforge.spi.model.Response
import org.devlive.metaforge.spi.model.RuntimeConfigure

class DolphinDBService : PluginService
{
    override fun validator(): String?
    {
        // Without FROM the driver returns the whole column as one vector object; select from a one-row table
        return "select version() as version from table(1 as x)"
    }

    override fun driver(): String
    {
        return "com.dolphindb.jdbc.Driver"
    }

    override fun connectType(): String?
    {
        return "dolphindb"
    }

    // The driver's metadata calls return no rows, so databases, tables and columns come from DolphinDB functions

    override fun getDatabases(configure: RuntimeConfigure): Response
    {
        return this.execute(configure, "select name as object_name, \"DATABASE\" as object_type, \"\" as object_comment " +
                "from table(getClusterDFSDatabases() as name)")
    }

    override fun getTables(configure: RuntimeConfigure, database: String): Response
    {
        return this.execute(configure, "select \"table\" as type_name, name as object_name, \"\" as object_comment " +
                "from table(getTables(database(${quote(database)})) as name)")
    }

    override fun getColumns(configure: RuntimeConfigure, database: String, table: String): Response
    {
        return this.execute(configure, "select \"column\" as type_name, name as object_name, typeString as object_data_type, " +
                "comment as object_comment from schema(loadTable(${quote(database)}, ${quote(table)})).colDefs")
    }

    private fun quote(value: String): String
    {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
