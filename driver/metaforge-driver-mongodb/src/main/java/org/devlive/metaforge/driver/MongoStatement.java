package org.devlive.metaforge.driver;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.devlive.metaforge.driver.iterable.InMemoryAggregateIterable;
import org.devlive.metaforge.driver.parser.MongoParser;
import org.devlive.metaforge.driver.parser.MongoShowParser;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "NP_NULL_PARAM_DEREF"})
public class MongoStatement
        implements Statement
{
    private final MongoConnection connection;
    private boolean isClosed = false;

    // Constructor
    // 构造函数
    public MongoStatement(MongoConnection connection)
    {
        this.connection = connection;
    }

    // Execute query and return ResultSet
    // 执行查询并返回ResultSet
    @Override
    public ResultSet executeQuery(String sql)
            throws SQLException
    {
        checkClosed();

        try {
            // Parse SQL to MongoDB query
            MongoParser parser = MongoParser.createParser(sql);
            if (parser instanceof MongoShowParser) {
                return executeShowStatement((MongoShowParser) parser);
            }

            Document query = parser.getQuery();
            if (query.containsKey("buildInfo")) {
                Document buildInfo = connection.getDatabase()
                        .runCommand(new Document("buildInfo", 1));

                Document versionDoc = new Document();
                versionDoc.put("version", buildInfo.getString("version"));
                return new MongoResultSet(new InMemoryAggregateIterable(List.of(versionDoc)));
            }

            String collectionName = parser.getCollection();
            log.debug("Executing query: {}", query);

            String[] dbAndTb = parser.getCollection().split("\\.");
            MongoDatabase db = connection.getDatabase();
            if (dbAndTb.length > 1) {
                db = connection.getClient().getDatabase(dbAndTb[0]);
                collectionName = dbAndTb[1];
            }

            MongoCollection<Document> collection = db.getCollection(collectionName);

            // Execute aggregate command
            @SuppressWarnings("unchecked")
            List<Document> pipeline = (List<Document>) query.get("pipeline");
            AggregateIterable<Document> result = collection.aggregate(pipeline);

            return new MongoResultSet(result);
        }
        catch (Exception e) {
            throw new SQLException("Failed to execute query", e);
        }
    }

    private ResultSet executeShowStatement(MongoShowParser parser)
            throws SQLException
    {
        try {
            switch (parser.getShowType()) {
                case DATABASES:
                    return handleShowDatabases(parser);
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

    private ResultSet handleShowDatabases(MongoShowParser parser)
    {
        List<Document> docs = connection.getClient().listDatabaseNames()
                .map(name -> new Document("name", name))
                .into(new ArrayList<>());
        return new MongoResultSet(new InMemoryAggregateIterable(docs));
    }

    private ResultSet handleShowTables(MongoShowParser parser)
    {
        MongoDatabase db = parser.getDatabase() != null ?
                connection.getClient().getDatabase(parser.getDatabase()) :
                connection.getDatabase();

        List<Document> docs = db.listCollectionNames()
                .map(name -> new Document("name", name))
                .into(new ArrayList<>());
        return new MongoResultSet(new InMemoryAggregateIterable(docs));
    }

    private ResultSet handleShowColumns(MongoShowParser parser)
    {
        String[] dbAndTb = parser.getCollection().split("\\.");
        String database = parser.getDatabase();
        String table = parser.getCollection();
        if (database == null && dbAndTb.length == 2) {
            database = dbAndTb[0];
            table = dbAndTb[1];
        }

        MongoDatabase db = connection.getClient().getDatabase(database);

        Document sample = db.getCollection(table)
                .find()
                .limit(1)
                .first();

        List<Document> docs = new ArrayList<>();
        if (sample != null) {
            sample.keySet().forEach(field ->
                    docs.add(new Document("name", field))
            );
        }
        return new MongoResultSet(new InMemoryAggregateIterable(docs));
    }

    private boolean matchesPattern(String value, String pattern)
    {
        if (pattern == null) {
            return true;
        }
        return value.matches(pattern.replace("%", ".*"));
    }

    private String getMongoFieldType(Object value)
    {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Integer) {
            return "int";
        }
        if (value instanceof Long) {
            return "long";
        }
        if (value instanceof Double) {
            return "double";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Document) {
            return "document";
        }
        if (value instanceof List) {
            return "array";
        }
        return value.getClass().getSimpleName();
    }

    // Execute update statement
    // 执行更新语句
    @Override
    public int executeUpdate(String sql)
            throws SQLException
    {
        throw new UnsupportedOperationException("Update operation not supported");
    }

    // Check if statement is closed
    // 检查语句是否已关闭
    private void checkClosed()
            throws SQLException
    {
        if (isClosed) {
            throw new SQLException("Statement is closed");
        }
    }

    // Close the statement
    // 关闭语句
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
