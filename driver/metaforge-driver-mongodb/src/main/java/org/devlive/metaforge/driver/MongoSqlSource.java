package org.devlive.metaforge.driver;

import com.mongodb.client.MongoClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.Document;
import org.devlive.metaforge.driver.sql.SourceColumn;
import org.devlive.metaforge.driver.sql.SqlSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB as the SQL driver foundation describes a source: databases, their collections as tables,
 * and as columns the fields of the first documents of a collection, typed by their values.
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "The client belongs to the connection that made this source")
public class MongoSqlSource
        implements SqlSource
{
    /** Documents read to find a collection's fields; MongoDB has no fixed schema. */
    static final int SAMPLE = 100;

    private final MongoClient client;

    public MongoSqlSource(MongoClient client)
    {
        this.client = client;
    }

    @Override
    public String productName()
    {
        return "MongoDB";
    }

    @Override
    public String productVersion()
    {
        return String.valueOf(client.getDatabase("admin").runCommand(new Document("buildInfo", 1)).get("version"));
    }

    @Override
    public List<String> databases()
    {
        List<String> databases = client.listDatabaseNames().into(new ArrayList<>());
        Collections.sort(databases);
        return databases;
    }

    @Override
    public List<String> tables(String database)
    {
        List<String> collections = client.getDatabase(database).listCollectionNames().into(new ArrayList<>());
        Collections.sort(collections);
        return collections;
    }

    @Override
    public List<SourceColumn> columns(String database, String table)
    {
        Map<String, String> fields = new LinkedHashMap<>();
        for (Document document : client.getDatabase(database).getCollection(table).find().limit(SAMPLE)) {
            document.forEach((name, value) -> fields.merge(name, type(value), (seen, next) -> seen.equals(next) || "NULL".equals(next) ? seen : "NULL".equals(seen) ? next : "MIXED"));
        }
        List<SourceColumn> columns = new ArrayList<>();
        fields.forEach((name, type) -> columns.add(SourceColumn.of(name, type)));
        return columns;
    }

    @Override
    public List<List<Object>> rows(String database, String table, long limit)
    {
        List<String> names = new ArrayList<>();
        columns(database, table).forEach(column -> names.add(column.getName()));
        List<List<Object>> rows = new ArrayList<>();
        for (Document document : client.getDatabase(database).getCollection(table).find().limit((int) Math.min(Integer.MAX_VALUE, limit))) {
            List<Object> row = new ArrayList<>();
            names.forEach(name -> row.add(document.get(name)));
            rows.add(row);
        }
        return rows;
    }

    /** The connection owns the client and closes it. */
    @Override
    public void close()
    {
    }

    static String type(Object value)
    {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "VARCHAR";
        }
        if (value instanceof Integer) {
            return "INTEGER";
        }
        if (value instanceof Long) {
            return "BIGINT";
        }
        if (value instanceof Double || value instanceof Float) {
            return "DOUBLE";
        }
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        if (value instanceof java.util.Date) {
            return "TIMESTAMP";
        }
        if (value instanceof List) {
            return "ARRAY";
        }
        if (value instanceof Document || value instanceof Map) {
            return "DOCUMENT";
        }
        return value.getClass().getSimpleName().toUpperCase(java.util.Locale.ROOT);
    }
}
