package org.devlive.metaforge;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectMetadata;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.devlive.metaforge.driver.sql.ConnectionUrl;
import org.devlive.metaforge.driver.sql.SourceColumn;
import org.devlive.metaforge.driver.sql.SqlSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Buckets as databases, top-level prefixes as tables and the objects under a prefix as rows. */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "The source owns its OSS client")
public final class AliOssSource
        implements SqlSource
{
    static final List<SourceColumn> COLUMNS = List.of(
            new SourceColumn("key", "VARCHAR", false, true, "Object key under the table prefix"),
            new SourceColumn("size", "BIGINT", false, false, "Size in bytes"),
            new SourceColumn("etag", "VARCHAR", false, false, "ETag"),
            new SourceColumn("last_modified", "TIMESTAMP", false, false, "Last modification time"),
            new SourceColumn("storage_class", "VARCHAR", false, false, "Storage class"),
            new SourceColumn("content", "VARCHAR", true, false, "Object text; read for objects up to contentLimit bytes"));
    private static final Set<String> WRITABLE = Set.of("key", "content");

    private final OSS client;
    private final String bucket;
    private final long maxRows;
    private final long contentLimit;

    AliOssSource(OSS client, String bucket, long maxRows, long contentLimit)
    {
        this.client = client;
        this.bucket = bucket == null || bucket.isEmpty() ? null : bucket;
        this.maxRows = maxRows;
        this.contentLimit = contentLimit;
    }

    static AliOssSource connect(ConnectionUrl url)
            throws SQLException
    {
        String user = url.get("user", null);
        String password = url.get("password", null);
        if (user == null || password == null) {
            throw new SQLException("Aliyun OSS needs the AccessKey ID as user and the AccessKey secret as password");
        }
        String endpoint = (Boolean.parseBoolean(url.get("ssl", "true")) ? "https://" : "http://") + url.getHosts();
        OSS client = new OSSClientBuilder().build(endpoint, user, password);
        AliOssSource source = new AliOssSource(client, url.getDatabase(), Long.parseLong(url.get("maxRows", "10000")),
                Long.parseLong(url.get("contentLimit", "0")));
        try {
            source.databases();
        }
        catch (SQLException error) {
            client.shutdown();
            throw error;
        }
        return source;
    }

    private interface Call<T>
    {
        T run()
                throws IOException;
    }

    private static <T> T call(String what, Call<T> call)
            throws SQLException
    {
        try {
            return call.run();
        }
        catch (OSSException error) {
            throw new SQLException(what + " failed: " + error.getErrorCode() + " " + error.getErrorMessage(), error);
        }
        catch (ClientException | IOException error) {
            throw new SQLException(what + " failed: " + error.getMessage(), error);
        }
    }

    private static String prefix(String table)
            throws SQLException
    {
        if (table.isEmpty() || table.contains("/")) {
            throw new SQLException("Invalid table name " + table);
        }
        return table + "/";
    }

    @Override
    public String productName()
    {
        return "Aliyun OSS";
    }

    /** OSS is a service without releases; its REST API version stands for it. */
    @Override
    public String productVersion()
    {
        return "2013-10-15";
    }

    /** Every bucket the key may list; only the bucket in the URL when the key may not list buckets. */
    @Override
    public List<String> databases()
            throws SQLException
    {
        try {
            return client.listBuckets().stream().map(Bucket::getName).sorted().collect(Collectors.toList());
        }
        catch (OSSException error) {
            if (bucket != null && "AccessDenied".equals(error.getErrorCode())) {
                call("Check bucket", () -> client.doesBucketExist(bucket));
                return List.of(bucket);
            }
            throw new SQLException("SHOW DATABASES failed: " + error.getErrorCode() + " " + error.getErrorMessage(), error);
        }
        catch (ClientException error) {
            throw new SQLException("Can not reach Aliyun OSS: " + error.getMessage(), error);
        }
    }

    @Override
    public void createDatabase(String database)
            throws SQLException
    {
        call("CREATE DATABASE", () -> client.createBucket(database));
    }

    /** OSS refuses to delete a bucket that still holds objects. */
    @Override
    public void dropDatabase(String database)
            throws SQLException
    {
        call("DROP DATABASE", () -> {
            client.deleteBucket(database);
            return null;
        });
    }

    @Override
    public List<String> tables(String database)
            throws SQLException
    {
        List<String> tables = new ArrayList<>();
        String token = null;
        do {
            ListObjectsV2Request request = new ListObjectsV2Request(database).withDelimiter("/").withMaxKeys(1000).withContinuationToken(token);
            ListObjectsV2Result result = call("SHOW TABLES", () -> client.listObjectsV2(request));
            for (String prefix : result.getCommonPrefixes()) {
                tables.add(prefix.substring(0, prefix.length() - 1));
            }
            token = result.isTruncated() ? result.getNextContinuationToken() : null;
        }
        while (token != null);
        return tables.stream().sorted().collect(Collectors.toList());
    }

    /** A table is its prefix: CREATE TABLE writes the empty folder object logs/. */
    @Override
    public void createTable(String database, String table, List<SourceColumn> columns)
            throws SQLException
    {
        Set<String> fixed = COLUMNS.stream().map(SourceColumn::getName).collect(Collectors.toSet());
        for (SourceColumn column : columns) {
            if (!fixed.contains(column.getName())) {
                throw new SQLException("A prefix's columns are fixed (" + String.join(", ", fixed) + "), not " + column.getName());
            }
        }
        String prefix = prefix(table);
        call("CREATE TABLE", () -> client.putObject(database, prefix, new ByteArrayInputStream(new byte[0])));
    }

    @Override
    public void dropTable(String database, String table)
            throws SQLException
    {
        List<String> keys = new ArrayList<>();
        for (OSSObjectSummary summary : objects(database, table, -1, "DROP TABLE")) {
            keys.add(summary.getKey());
        }
        keys.add(prefix(table));
        delete(database, keys, "DROP TABLE");
    }

    private void delete(String database, List<String> keys, String what)
            throws SQLException
    {
        for (int i = 0; i < keys.size(); i += 1000) {
            List<String> batch = keys.subList(i, Math.min(keys.size(), i + 1000));
            call(what, () -> client.deleteObjects(new DeleteObjectsRequest(database).withKeys(new ArrayList<>(batch)).withQuiet(true)));
        }
    }

    @Override
    public List<SourceColumn> columns(String database, String table)
    {
        return COLUMNS;
    }

    /** Objects under the table prefix, the folder object itself left out, up to limit (or maxRows). */
    private List<OSSObjectSummary> objects(String database, String table, long limit, String what)
            throws SQLException
    {
        String prefix = prefix(table);
        long wanted = limit < 0 ? Long.MAX_VALUE : limit;
        List<OSSObjectSummary> objects = new ArrayList<>();
        String token = null;
        do {
            ListObjectsV2Request request = new ListObjectsV2Request(database).withPrefix(prefix).withMaxKeys(1000).withContinuationToken(token);
            ListObjectsV2Result result = call(what, () -> client.listObjectsV2(request));
            for (OSSObjectSummary summary : result.getObjectSummaries()) {
                if (!summary.getKey().equals(prefix) && objects.size() < wanted) {
                    objects.add(summary);
                }
            }
            token = result.isTruncated() && objects.size() < wanted ? result.getNextContinuationToken() : null;
        }
        while (token != null);
        return objects;
    }

    private List<Object> row(String database, OSSObjectSummary summary, String prefix)
            throws SQLException
    {
        String content = null;
        if (summary.getSize() <= contentLimit) {
            content = call("Read " + summary.getKey(), () -> {
                try (OSSObject object = client.getObject(database, summary.getKey()); InputStream in = object.getObjectContent()) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            });
        }
        return Arrays.asList(summary.getKey().substring(prefix.length()), summary.getSize(), summary.getETag(),
                summary.getLastModified() == null ? null : new Timestamp(summary.getLastModified().getTime()), summary.getStorageClass(), content);
    }

    @Override
    public List<List<Object>> rows(String database, String table, long limit)
            throws SQLException
    {
        String prefix = prefix(table);
        List<List<Object>> rows = new ArrayList<>();
        for (OSSObjectSummary summary : objects(database, table, limit < 0 ? maxRows : Math.min(limit, maxRows), "SELECT")) {
            rows.add(row(database, summary, prefix));
        }
        return rows;
    }

    private Map<String, Object> named(String database, OSSObjectSummary summary, String prefix)
            throws SQLException
    {
        Map<String, Object> named = new LinkedHashMap<>();
        List<Object> values = row(database, summary, prefix);
        for (int i = 0; i < COLUMNS.size(); i++) {
            named.put(COLUMNS.get(i).getName(), values.get(i));
        }
        return named;
    }

    private void put(String database, String key, Object content)
            throws SQLException
    {
        byte[] bytes = content == null ? new byte[0] : String.valueOf(content).getBytes(StandardCharsets.UTF_8);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        call("Write " + key, () -> client.putObject(database, key, new ByteArrayInputStream(bytes), metadata));
    }

    /** Writes one object per row from key and content. */
    @Override
    public int insert(String database, String table, List<String> columns, List<List<Object>> rows)
            throws SQLException
    {
        if (!columns.contains("key")) {
            throw new SQLException("INSERT needs the key of the object");
        }
        for (String column : columns) {
            if (!WRITABLE.contains(column)) {
                throw new SQLException("Column " + column + " can not be written; writable are key and content");
            }
        }
        String prefix = prefix(table);
        for (List<Object> row : rows) {
            Object key = row.get(columns.indexOf("key"));
            if (key == null || String.valueOf(key).isEmpty()) {
                throw new SQLException("An object needs a key");
            }
            put(database, prefix + key, columns.contains("content") ? row.get(columns.indexOf("content")) : null);
        }
        return rows.size();
    }

    /** SET content rewrites the object; SET key copies it to the new key and removes the old one. */
    @Override
    public int update(String database, String table, Map<String, Object> values, Predicate<Map<String, Object>> filter)
            throws SQLException
    {
        for (String column : values.keySet()) {
            if (!WRITABLE.contains(column)) {
                throw new SQLException("Column " + column + " can not be changed; writable are key and content");
            }
        }
        String prefix = prefix(table);
        int count = 0;
        for (OSSObjectSummary summary : objects(database, table, maxRows, "UPDATE")) {
            if (!filter.test(named(database, summary, prefix))) {
                continue;
            }
            String key = summary.getKey();
            if (values.containsKey("content")) {
                put(database, key, values.get("content"));
            }
            if (values.get("key") != null) {
                String target = prefix + values.get("key");
                if (!target.equals(key)) {
                    call("Rename " + key, () -> client.copyObject(database, key, database, target));
                    call("Rename " + key, () -> {
                        client.deleteObject(database, key);
                        return null;
                    });
                }
            }
            count++;
        }
        return count;
    }

    @Override
    public int delete(String database, String table, Predicate<Map<String, Object>> filter)
            throws SQLException
    {
        String prefix = prefix(table);
        List<String> matched = new ArrayList<>();
        for (OSSObjectSummary summary : objects(database, table, maxRows, "DELETE")) {
            if (filter.test(named(database, summary, prefix))) {
                matched.add(summary.getKey());
            }
        }
        delete(database, matched, "DELETE");
        return matched.size();
    }

    @Override
    public void close()
    {
        client.shutdown();
    }
}
