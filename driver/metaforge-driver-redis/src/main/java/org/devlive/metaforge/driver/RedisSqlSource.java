package org.devlive.metaforge.driver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.driver.sql.SourceColumn;
import org.devlive.metaforge.driver.sql.SqlSource;
import org.redisson.api.RedissonClient;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Redis as the SQL driver foundation describes a source, the way MySQL is browsed: the numbered
 * databases, their keys as tables, and each key as one row of key, type and value.
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "The connection owns the clients this source reads through")
public class RedisSqlSource
        implements SqlSource
{
    /** Keys listed per database; a larger keyspace is narrowed with SHOW TABLES LIKE or SELECT ... FROM `pattern*`. */
    static final int KEYS = 10000;
    static final List<String> COLUMNS = List.of("key", "type", "value");

    private final RedisConnection connection;

    public RedisSqlSource(RedisConnection connection)
    {
        this.connection = connection;
    }

    @Override
    public String productName()
    {
        return "Redis";
    }

    @Override
    public String productVersion()
            throws SQLException
    {
        return RedisStatement.serverVersion(connection.getClient(connection.getDatabase()));
    }

    /** 0 .. databases-1 from CONFIG GET databases; 16, Redis' default, when CONFIG is not allowed. */
    @Override
    public List<String> databases()
            throws SQLException
    {
        int count = 16;
        if (connection.isCluster()) {
            count = 1;
        }
        else {
            try {
                List<Map<String, Object>> reply = RedisCommands.run(connection.getClient(connection.getDatabase()), "CONFIG GET databases");
                if (reply.size() == 2) {
                    count = Integer.parseInt(String.valueOf(reply.get(1).get(RedisCommands.COLUMN)));
                }
            }
            catch (RuntimeException ignored) {
                // Managed Redis often refuses CONFIG; the default count still lists the usual databases.
            }
        }
        List<String> databases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            databases.add(String.valueOf(i));
        }
        return databases;
    }

    @Override
    public List<String> tables(String database)
            throws SQLException
    {
        RedissonClient client = connection.getClient(number(database));
        List<String> keys = new ArrayList<>();
        for (String key : client.getKeys().getKeys()) {
            if (keys.size() >= KEYS) {
                break;
            }
            keys.add(key);
        }
        keys.sort(String::compareTo);
        return keys;
    }

    @Override
    public List<SourceColumn> columns(String database, String table)
    {
        return Arrays.asList(SourceColumn.of("key", "VARCHAR"), SourceColumn.of("type", "VARCHAR"), SourceColumn.of("value", "VARCHAR"));
    }

    @Override
    public List<List<Object>> rows(String database, String table, long limit)
            throws SQLException
    {
        RedissonClient client = connection.getClient(number(database));
        List<List<Object>> rows = new ArrayList<>();
        if (limit > 0 && client.getKeys().countExists(table) > 0) {
            rows.add(Arrays.asList(table, RedisStatement.keyType(client, table), String.valueOf(RedisStatement.keyValue(client, table))));
        }
        return rows;
    }

    /** The connection owns the clients and closes them. */
    @Override
    public void close()
    {
    }

    private static int number(String database)
            throws SQLException
    {
        try {
            return Integer.parseInt(database.trim());
        }
        catch (RuntimeException e) {
            throw new SQLException("Redis databases are numbers: " + database);
        }
    }
}
