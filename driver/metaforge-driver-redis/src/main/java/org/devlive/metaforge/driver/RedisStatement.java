package org.devlive.metaforge.driver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.driver.parser.RedisParser;
import org.devlive.metaforge.driver.parser.RedisShowParser;
import org.redisson.api.RKeys;
import org.redisson.api.RType;
import org.redisson.api.RedissonClient;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "NP_NULL_PARAM_DEREF"})
public class RedisStatement
        implements Statement
{
    private final RedisConnection connection;
    private boolean isClosed = false;
    private ResultSet resultSet;

    public RedisStatement(RedisConnection connection)
    {
        this.connection = connection;
    }

    @Override
    public ResultSet executeQuery(String sql)
            throws SQLException
    {
        checkClosed();

        try {
            if (RedisCommands.isCommand(sql)) {
                return new RedisResultSet(RedisCommands.run(connection.getClient(), sql));
            }
            RedisParser parser = RedisParser.createParser(sql);
            if (parser instanceof RedisShowParser) {
                return executeShowStatement((RedisShowParser) parser);
            }

            Map<String, Object> query = parser.getQuery();
            if (query != null && query.containsKey("serverInfo")) {
                return handleServerInfo();
            }

            return executeSelectStatement(parser);
        }
        catch (Exception e) {
            throw new SQLException("Failed to execute query", e);
        }
    }

    /** SELECT version(): the server's redis_version from INFO server. */
    private ResultSet handleServerInfo()
    {
        String version = "";
        for (Map<String, Object> line : RedisCommands.run(connection.getClient(), "INFO server")) {
            for (String part : String.valueOf(line.get(RedisCommands.COLUMN)).split("\\r?\\n")) {
                if (part.startsWith("redis_version:")) {
                    version = part.substring("redis_version:".length()).trim();
                }
            }
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("version", version);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row);
        return new RedisResultSet(rows);
    }

    /**
     * SELECT ... FROM key: an exact key, a pattern with * or ? (`test:*`), or else the keys starting
     * with the name; FROM * reads every key.
     * Each key is a row of key, type and value, and the selected columns are kept in order.
     */
    private ResultSet executeSelectStatement(RedisParser parser)
            throws SQLException
    {
        RedissonClient client = connection.getClient();
        String collection = unquote(parser.getCollection());
        Iterable<String> keys;
        if (collection == null || collection.isEmpty() || collection.equals("*")) {
            keys = client.getKeys().getKeys();
        }
        else if (collection.contains("*") || collection.contains("?")) {
            keys = client.getKeys().getKeysByPattern(collection);
        }
        else if (client.getKeys().countExists(collection) > 0) {
            keys = List.of(collection);
        }
        else {
            // Not a key: the keys starting with it, as FROM test always read test:*.
            keys = client.getKeys().getKeysByPattern(collection + "*");
        }

        List<String> columns = columns(parser.getFields());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String key : keys) {
            Map<String, Object> all = new LinkedHashMap<>();
            all.put("key", key);
            all.put("type", getKeyType(client, key));
            all.put("value", getKeyValue(client, key));
            Map<String, Object> row = new LinkedHashMap<>();
            columns.forEach(column -> row.put(column, all.get(column)));
            rows.add(row);
        }
        return new RedisResultSet(columns, rows);
    }

    private static final List<String> COLUMNS = List.of("key", "type", "value");

    private static List<String> columns(List<String> fields)
            throws SQLException
    {
        if (fields == null || fields.isEmpty() || fields.stream().anyMatch(field -> "*".equals(unquote(field)))) {
            return COLUMNS;
        }
        List<String> columns = new ArrayList<>();
        for (String field : fields) {
            String name = unquote(field).toLowerCase(Locale.ROOT);
            if (!COLUMNS.contains(name)) {
                throw new SQLException("Unknown column " + field + "; a key has the columns key, type and value");
            }
            columns.add(name);
        }
        return columns;
    }

    /** Keys and columns may be quoted: `test:list`, "test:list" or 'test:list'. */
    private static String unquote(String name)
    {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '`' || first == '"' || first == '\'') && first == last) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }

    private String getKeyType(RedissonClient client, String key)
    {
        RType type = client.getKeys().getType(key);
        return type != null ? type.name() : "NONE";
    }

    private Object getKeyValue(RedissonClient client, String key)
    {
        RType type = client.getKeys().getType(key);
        if (type == null) {
            return null;
        }

        String typeName = type.name();
        if ("STRING".equals(typeName) || "OBJECT".equals(typeName)) {
            return client.getBucket(key).get();
        }
        else if ("LIST".equals(typeName)) {
            return client.getList(key).readAll();
        }
        else if ("SET".equals(typeName)) {
            return client.getSet(key).readAll();
        }
        else if ("ZSET".equals(typeName)) {
            return client.getScoredSortedSet(key).readAll();
        }
        else if ("MAP".equals(typeName) || "HASH".equals(typeName)) {
            return client.getMap(key).readAllMap();
        }
        else {
            return "[" + typeName + "]";
        }
    }

    private ResultSet executeShowStatement(RedisShowParser parser)
            throws SQLException
    {
        try {
            switch (parser.getShowType()) {
                case DATABASES:
                    return handleShowDatabases();
                case TABLES:
                    return handleShowTables(parser);
                case COLUMNS:
                    return handleShowColumns(parser);
                default:
                    throw new SQLException("Unsupported SHOW command type");
            }
        }
        catch (Exception e) {
            throw new SQLException("Failed to execute SHOW command", e);
        }
    }

    private ResultSet handleShowDatabases()
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", String.valueOf(i));
            rows.add(row);
        }
        return new RedisResultSet(rows);
    }

    private ResultSet handleShowTables(RedisShowParser parser)
    {
        RedissonClient client = connection.getClient();
        List<Map<String, Object>> rows = new ArrayList<>();

        String pattern = parser.getPattern() != null ? parser.getPattern() : "*";
        RKeys keys = client.getKeys();

        for (String key : keys.getKeysByPattern(pattern)) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", key);
            rows.add(row);
        }

        return new RedisResultSet(rows);
    }

    private ResultSet handleShowColumns(RedisShowParser parser)
    {
        RedissonClient client = connection.getClient();
        String tableName = parser.getCollection();
        List<Map<String, Object>> rows = new ArrayList<>();

        if (tableName != null) {
            RType type = client.getKeys().getType(tableName);
            if (type != null && ("MAP".equals(type.name()) || "HASH".equals(type.name()))) {
                for (Object field : client.getMap(tableName).keySet()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", field.toString());
                    rows.add(row);
                }
            }
            else {
                Map<String, Object> row = new HashMap<>();
                row.put("name", "value");
                rows.add(row);
            }
        }

        return new RedisResultSet(rows);
    }

    @Override
    public int executeUpdate(String sql)
            throws SQLException
    {
        checkClosed();
        if (!RedisCommands.isCommand(sql)) {
            throw new SQLFeatureNotSupportedException("Only Redis commands can update: " + sql);
        }
        try {
            // The released driver answered with the number of reply values.
            return RedisCommands.run(connection.getClient(), sql).size();
        }
        catch (Exception e) {
            throw new SQLException("Failed to execute command", e);
        }
    }

    private void checkClosed()
            throws SQLException
    {
        if (isClosed) {
            throw new SQLException("Statement is closed");
        }
    }

    @Override
    public void close()
            throws SQLException
    {
        isClosed = true;
    }

    @Override
    public int getMaxFieldSize()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max)
            throws SQLException
    {}

    @Override
    public int getMaxRows()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void setMaxRows(int max)
            throws SQLException
    {}

    @Override
    public void setEscapeProcessing(boolean enable)
            throws SQLException
    {}

    @Override
    public int getQueryTimeout()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void setQueryTimeout(int seconds)
            throws SQLException
    {}

    @Override
    public void cancel()
            throws SQLException
    {}

    @Override
    public SQLWarning getWarnings()
            throws SQLException
    {
        return null;
    }

    @Override
    public void clearWarnings()
            throws SQLException
    {}

    @Override
    public void setCursorName(String name)
            throws SQLException
    {}

    @Override
    public boolean execute(String sql)
            throws SQLException
    {
        resultSet = executeQuery(sql);
        return true;
    }

    @Override
    public ResultSet getResultSet()
            throws SQLException
    {
        return resultSet;
    }

    @Override
    public int getUpdateCount()
            throws SQLException
    {
        // execute() always leaves a result set, so there is no update count.
        return -1;
    }

    @Override
    public boolean getMoreResults()
            throws SQLException
    {
        return false;
    }

    @Override
    public void setFetchDirection(int direction)
            throws SQLException
    {}

    @Override
    public int getFetchDirection()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void setFetchSize(int rows)
            throws SQLException
    {}

    @Override
    public int getFetchSize()
            throws SQLException
    {
        return 0;
    }

    @Override
    public int getResultSetConcurrency()
            throws SQLException
    {
        return 0;
    }

    @Override
    public int getResultSetType()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void addBatch(String sql)
            throws SQLException
    {}

    @Override
    public void clearBatch()
            throws SQLException
    {}

    @Override
    public int[] executeBatch()
            throws SQLException
    {
        return new int[0];
    }

    @Override
    public Connection getConnection()
            throws SQLException
    {
        return null;
    }

    @Override
    public boolean getMoreResults(int current)
            throws SQLException
    {
        return false;
    }

    @Override
    public ResultSet getGeneratedKeys()
            throws SQLException
    {
        return null;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys)
            throws SQLException
    {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes)
            throws SQLException
    {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames)
            throws SQLException
    {
        return 0;
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys)
            throws SQLException
    {
        return false;
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes)
            throws SQLException
    {
        return false;
    }

    @Override
    public boolean execute(String sql, String[] columnNames)
            throws SQLException
    {
        return false;
    }

    @Override
    public int getResultSetHoldability()
            throws SQLException
    {
        return 0;
    }

    @Override
    public boolean isClosed()
            throws SQLException
    {
        return false;
    }

    @Override
    public void setPoolable(boolean poolable)
            throws SQLException
    {}

    @Override
    public boolean isPoolable()
            throws SQLException
    {
        return false;
    }

    @Override
    public void closeOnCompletion()
            throws SQLException
    {}

    @Override
    public boolean isCloseOnCompletion()
            throws SQLException
    {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface)
            throws SQLException
    {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
            throws SQLException
    {
        return false;
    }
}
