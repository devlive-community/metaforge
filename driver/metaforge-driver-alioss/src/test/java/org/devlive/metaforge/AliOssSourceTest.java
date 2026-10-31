package org.devlive.metaforge;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.CopyObjectResult;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.VoidResult;
import org.devlive.metaforge.driver.sql.Rows;
import org.devlive.metaforge.driver.sql.SqlEngine;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** The OSS mapping against an in-memory OSS: OSS has no server to run locally. */
public class AliOssSourceTest
{
    /** bucket -> key -> content */
    private final Map<String, TreeMap<String, byte[]>> buckets = new TreeMap<>();
    private boolean listBucketsDenied;

    private OSS fake()
    {
        return (OSS) Proxy.newProxyInstance(OSS.class.getClassLoader(), new Class<?>[] {OSS.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "listBuckets":
                    if (listBucketsDenied) {
                        throw new OSSException("denied", "AccessDenied", null, null, null, null, null);
                    }
                    return buckets.keySet().stream().map(Bucket::new).collect(Collectors.toList());
                case "doesBucketExist":
                    return buckets.containsKey((String) args[0]);
                case "createBucket":
                    buckets.put((String) args[0], new TreeMap<>());
                    return new Bucket((String) args[0]);
                case "deleteBucket":
                    if (!buckets.get((String) args[0]).isEmpty()) {
                        throw new OSSException("not empty", "BucketNotEmpty", null, null, null, null, null);
                    }
                    buckets.remove((String) args[0]);
                    return new VoidResult();
                case "listObjectsV2":
                    return list((ListObjectsV2Request) args[0]);
                case "putObject":
                    buckets.get((String) args[0]).put((String) args[1], ((InputStream) args[2]).readAllBytes());
                    return new PutObjectResult();
                case "deleteObjects":
                    DeleteObjectsRequest delete = (DeleteObjectsRequest) args[0];
                    delete.getKeys().forEach(buckets.get(delete.getBucketName())::remove);
                    return new DeleteObjectsResult();
                case "deleteObject":
                    buckets.get((String) args[0]).remove((String) args[1]);
                    return new VoidResult();
                case "copyObject":
                    buckets.get((String) args[2]).put((String) args[3], buckets.get((String) args[0]).get((String) args[1]));
                    return new CopyObjectResult();
                case "getObject":
                    OSSObject object = new OSSObject();
                    object.setObjectContent(new ByteArrayInputStream(buckets.get((String) args[0]).get((String) args[1])));
                    return object;
                case "shutdown":
                    return null;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
    }

    private ListObjectsV2Result list(ListObjectsV2Request request)
    {
        ListObjectsV2Result result = new ListObjectsV2Result();
        String prefix = request.getPrefix() == null ? "" : request.getPrefix();
        for (Map.Entry<String, byte[]> entry : buckets.get(request.getBucketName()).entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(prefix)) {
                continue;
            }
            int slash = key.indexOf('/', prefix.length());
            if (request.getDelimiter() != null && slash >= 0) {
                String common = key.substring(0, slash + 1);
                if (!result.getCommonPrefixes().contains(common)) {
                    result.addCommonPrefix(common);
                }
                continue;
            }
            OSSObjectSummary summary = new OSSObjectSummary();
            summary.setKey(key);
            summary.setSize(entry.getValue().length);
            summary.setETag("etag-" + key.hashCode());
            summary.setLastModified(new Date(0));
            summary.setStorageClass("Standard");
            result.addObjectSummary(summary);
        }
        return result;
    }

    private static List<Object> column(Rows rows, String label)
    {
        int index = rows.getLabels().indexOf(label);
        return rows.getValues().stream().map(row -> row.get(index)).collect(Collectors.toList());
    }

    @Test
    public void bucketsPrefixesAndObjectsAreManagedWithSql()
            throws SQLException, IOException
    {
        SqlEngine engine = new SqlEngine(new AliOssSource(fake(), null, 10000, 1024), null);
        engine.execute("CREATE DATABASE lake");
        Assert.assertEquals(List.of("lake"), column(engine.execute("SHOW DATABASES"), "Database"));
        engine.execute("CREATE TABLE lake.logs (`key` VARCHAR, content VARCHAR)");
        Assert.assertTrue(buckets.get("lake").containsKey("logs/"));
        Assert.assertEquals(List.of("logs"), column(engine.execute("SHOW TABLES FROM lake"), "Table"));

        Assert.assertEquals(2, engine.execute("INSERT INTO lake.logs (`key`, content) VALUES ('a.log', 'hello'), ('2026/b.log', 'world')").getUpdateCount());
        Rows rows = engine.execute("SELECT `key`, size, content FROM lake.logs ORDER BY `key`");
        Assert.assertEquals(List.of("2026/b.log", "a.log"), column(rows, "key"));
        Assert.assertEquals(List.of("world", "hello"), column(rows, "content"));
        Assert.assertEquals(List.of(5L, 5L), column(rows, "size"));

        Assert.assertEquals(1, engine.execute("UPDATE lake.logs SET `key` = 'c.log' WHERE `key` = 'a.log'").getUpdateCount());
        Assert.assertTrue(buckets.get("lake").containsKey("logs/c.log"));
        Assert.assertFalse(buckets.get("lake").containsKey("logs/a.log"));
        engine.execute("UPDATE lake.logs SET content = 'changed' WHERE `key` = 'c.log'");
        Assert.assertEquals("changed", new String(buckets.get("lake").get("logs/c.log"), StandardCharsets.UTF_8));
        Assert.assertEquals(1, engine.execute("DELETE FROM lake.logs WHERE `key` LIKE '2026/%'").getUpdateCount());
        Assert.assertEquals(List.of("c.log"), column(engine.execute("SELECT `key` FROM lake.logs"), "key"));

        Assert.assertThrows(SQLException.class, () -> engine.execute("ALTER TABLE lake.logs ADD COLUMN extra VARCHAR"));
        Assert.assertThrows(SQLException.class, () -> engine.execute("INSERT INTO lake.logs (`key`, size) VALUES ('x', 1)"));
        Assert.assertThrows(SQLException.class, () -> engine.execute("DROP DATABASE lake"));

        engine.execute("DROP TABLE lake.logs");
        Assert.assertTrue(buckets.get("lake").isEmpty());
        engine.execute("DROP DATABASE lake");
        Assert.assertTrue(buckets.isEmpty());
    }

    @Test
    public void aKeyThatMayNotListBucketsSeesItsOwnBucket()
            throws SQLException
    {
        buckets.put("own", new TreeMap<>());
        listBucketsDenied = true;
        Assert.assertEquals(List.of("own"), new AliOssSource(fake(), "own", 10000, 0).databases());
        Assert.assertThrows(SQLException.class, () -> new AliOssSource(fake(), null, 10000, 0).databases());
    }

    @Test
    public void contentIsReadOnlyUpToTheLimit()
            throws SQLException
    {
        buckets.put("lake", new TreeMap<>());
        buckets.get("lake").put("logs/", new byte[0]);
        buckets.get("lake").put("logs/a.log", "hello".getBytes(StandardCharsets.UTF_8));
        List<Object> row = new AliOssSource(fake(), null, 10000, 0).rows("lake", "logs", -1).get(0);
        Assert.assertEquals("a.log", row.get(0));
        Assert.assertNull(row.get(5));
        Assert.assertEquals(new ArrayList<>(List.of("hello")), List.of(new AliOssSource(fake(), null, 10000, 5).rows("lake", "logs", -1).get(0).get(5)));
    }
}
