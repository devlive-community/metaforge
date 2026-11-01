package org.devlive.metaforge.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.connection.JdbcUrlGuard;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.nio.file.Paths;
import java.util.regex.Pattern;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class DuckDBService
        implements PluginService
{
    // A directory on the server; no characters that would start URL options
    private static final Pattern SAFE_DIRECTORY = Pattern.compile("[^\\s?&#;=%'\"`]+");

    @Override
    public String driver()
    {
        return "org.duckdb.DuckDBDriver";
    }

    @Override
    public String connectType()
    {
        return "duckdb";
    }

    /**
     * The host field names the directory and the database field the file in it, so the URL is a
     * file path rather than host:port.
     */
    @Override
    public String url(RuntimeConfigure configure)
    {
        if (configure.getUrl().isPresent()) {
            return configure.getUrl().get();
        }
        String directory = configure.getHost();
        if (directory == null || !SAFE_DIRECTORY.matcher(directory).matches()) {
            throw new IllegalArgumentException("Invalid directory for a DuckDB database: " + directory);
        }
        String database = configure.getDatabase().orElse("local");
        JdbcUrlGuard.validateDatabase(database);
        return "jdbc:duckdb:" + Paths.get(directory, database);
    }

    /**
     * DuckDB reports tables as BASE TABLE, a type JDBC metadata lookups for TABLE miss, so read them
     * from information_schema.
     */
    @Override
    public Response getTables(RuntimeConfigure configure, String database)
    {
        String sql = "SELECT\n" +
                "    CASE WHEN table_type = 'VIEW' THEN 'view' ELSE 'table' END AS type_name,\n" +
                "    table_name AS object_name,\n" +
                "    '' AS object_comment\n" +
                "FROM information_schema.tables\n" +
                "WHERE table_schema = '{0}'\n" +
                "ORDER BY table_name";
        return this.execute(configure, sql.replace("{0}", database.replace("'", "''")));
    }
}
