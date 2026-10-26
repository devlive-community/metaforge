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
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "NP_NULL_PARAM_DEREF"})
public class RedisStatement
        implements Statement
{
    private final RedisConnection connection;
    private boolean isClosed = false;

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

    private ResultSet handleServerInfo()
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("info", "Redis Server");
        rows.add(row);
        return new RedisResultSet(rows);
    }

    private ResultSet executeSelectStatement(RedisParser parser)
    {
        RedissonClient client = connection.getClient();
        String collection = parser.getCollection();
        List<Map<String, Object>> rows = new ArrayList<>();

        if (collection == null || collection.equals("*")) {
            RKeys keys = client.getKeys();
            for (String key : keys.getKeys()) {
                Map<String, Object> row = new HashMap<>();
                row.put("key", key);
                row.put("type", getKeyType(client, key));
                row.put("value", getKeyValue(client, key));
                rows.add(row);
            }
        }
        else {
            RKeys keys = client.getKeys();
            String pattern = collection.contains("*") || collection.contains("?") ? collection : collection + "*";
            for (String key : keys.getKeysByPattern(pattern)) {
                Map<String, Object> row = new HashMap<>();
                row.put("key", key);
                row.put("type", getKeyType(client, key));
                row.put("value", getKeyValue(client, key));
                rows.add(row);
            }
        }

        return new RedisResultSet(rows);
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
        throw new UnsupportedOperationException("Update operation not supported");
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
        return false;
    }

    @Override
    public ResultSet getResultSet()
            throws SQLException
    {
        return null;
    }

    @Override
    public int getUpdateCount()
            throws SQLException
    {
        return 0;
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
