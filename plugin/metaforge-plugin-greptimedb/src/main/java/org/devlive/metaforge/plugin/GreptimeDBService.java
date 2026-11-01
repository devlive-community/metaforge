package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

/** GreptimeDB through the MySQL JDBC driver on its MySQL protocol port; its information_schema describes databases, tables and columns. */
public class GreptimeDBService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String connectType()
    {
        return "mysql";
    }

    @Override
    public Response getTables(RuntimeConfigure configure, String database)
    {
        String sql = "SELECT CASE WHEN table_type = 'VIEW' THEN 'view' ELSE 'table' END AS type_name, table_name AS object_name, '' AS object_comment\n" +
                "FROM information_schema.tables\n" +
                "WHERE table_schema = '{0}'\n" +
                "ORDER BY table_name";
        return this.execute(configure, sql.replace("{0}", database.replace("'", "''")));
    }

    @Override
    public Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        String sql = "SELECT 'column' AS type_name, column_name AS object_name, data_type AS object_data_type, is_nullable AS object_nullable,\n" +
                "    column_default AS object_default_value, '' AS object_comment, 0 AS object_position, semantic_type AS object_definition\n" +
                "FROM information_schema.columns\n" +
                "WHERE table_schema = '{0}' AND table_name = '{1}'";
        return this.execute(configure, sql.replace("{0}", database.replace("'", "''")).replace("{1}", table.replace("'", "''")));
    }
}
