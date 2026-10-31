package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

/**
 * QuestDB through the PostgreSQL JDBC driver on its PostgreSQL wire port. A QuestDB server holds
 * one database, so the plugin's database is that one and its tables come from {@code tables()}.
 */
public class QuestDBService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "org.postgresql.Driver";
    }

    @Override
    public String connectType()
    {
        return "postgresql";
    }

    @Override
    public String validator()
    {
        return "SELECT build() AS version";
    }

    /** The PostgreSQL driver needs a database in the URL; QuestDB answers to its own, qdb. */
    @Override
    public String url(RuntimeConfigure configure)
    {
        String url = PluginService.super.url(configure);
        if (configure.getUrl().isPresent() || configure.getDatabase().isPresent()) {
            return url;
        }
        return url.replaceFirst("^(jdbc:postgresql://[^/?]+)(\\?|$)", "$1/qdb$2");
    }

    @Override
    public Response getDatabases(RuntimeConfigure configure)
    {
        return this.execute(configure, "SELECT current_database() AS object_name, 'DATABASE' AS object_type, '' AS object_comment");
    }

    @Override
    public Response getTables(RuntimeConfigure configure, String database)
    {
        return this.execute(configure, "SELECT 'table' AS type_name, table_name AS object_name, '' AS object_comment FROM tables() ORDER BY table_name");
    }

    @Override
    public Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        String sql = "SELECT 'column' AS type_name, \"column\" AS object_name, type AS object_data_type, 'YES' AS object_nullable,\n" +
                "    '' AS object_default_value, '' AS object_comment, 0 AS object_position,\n" +
                "    CASE WHEN designated THEN 'DESIGNATED TIMESTAMP' ELSE '' END AS object_definition\n" +
                "FROM table_columns('{0}')";
        return this.execute(configure, sql.replace("{0}", table.replace("'", "''")));
    }
}
