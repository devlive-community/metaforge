package org.devlive.metaforge.plugin;

import org.devlive.metaforge.spi.model.ManagementDetail;
import org.devlive.metaforge.spi.model.ManagementEntry;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisManagementTest
{
    private RedissonClient client;
    private RuntimeConfigure configure;
    private String prefix;
    private final RedisManagement management = new RedisManagement();

    @Before
    public void prepare()
    {
        String port = System.getProperty("metaforge.redis.test.port");
        Assume.assumeTrue("An isolated Redis test server is required", port != null);
        prefix = "mf-native-test-" + UUID.randomUUID();
        Config config = new Config();
        config.setThreads(2).setNettyThreads(2);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port).setPassword("metaforge-native-test")
                .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        client = Redisson.create(config);
        configure = new RuntimeConfigure();
        configure.setHost("127.0.0.1");
        configure.setPort(Integer.parseInt(port));
        configure.setDatabase(Optional.of("0"));
        configure.setPassword(Optional.of("metaforge-native-test"));
    }

    @After
    public void cleanup()
    {
        if (client != null) {
            client.getKeys().deleteByPattern(prefix + ":*");
            client.shutdown();
        }
    }

    private ManagementRequest request(String name)
    {
        ManagementRequest request = new ManagementRequest();
        request.setNamespace("0");
        if (name != null) {
            request.setKey("k:" + Base64.getUrlEncoder().withoutPadding().encodeToString((prefix + ":" + name).getBytes(StandardCharsets.UTF_8)));
        }
        return request;
    }

    @Test
    public void scanUsesRealCursorsAndReturnsEveryMatchingKeyWithoutValues()
    {
        client.getBucket(prefix + ":text", StringCodec.INSTANCE).set("value", 60, TimeUnit.SECONDS);
        client.getList(prefix + ":list", StringCodec.INSTANCE).addAll(List.of("a", "b"));
        client.getMap(prefix + ":hash", StringCodec.INSTANCE).put("field", "value");
        client.getSet(prefix + ":set", StringCodec.INSTANCE).add("member");
        client.getScoredSortedSet(prefix + ":zset", StringCodec.INSTANCE).add(1.5, "member");
        ManagementRequest request = request(null);
        request.setPrefix(prefix + ":*");
        request.setLimit(2);
        Set<String> names = new HashSet<>();
        ManagementPage page;
        int batches = 0;
        do {
            page = management.list(configure, request);
            for (ManagementEntry entry : page.getEntries()) {
                names.add(entry.getName());
                Assert.assertNotNull(entry.getTtl());
                Assert.assertTrue(entry.getId().startsWith("k:"));
            }
            request.setCursor(page.getNextCursor());
            Assert.assertTrue(++batches < 1000);
        } while (page.isHasMore());
        Assert.assertEquals(Set.of(prefix + ":text", prefix + ":list", prefix + ":hash", prefix + ":set", prefix + ":zset"), names);
    }

    @Test
    public void stringsAndBinaryValuesHaveBoundedHonestPreviews()
    {
        client.getBucket(prefix + ":long", StringCodec.INSTANCE).set("a".repeat(70000));
        ManagementDetail detail = management.detail(configure, request("long"));
        Assert.assertEquals(70000, detail.getEntry().getSize());
        Assert.assertEquals(65536, detail.getContent().length());
        Assert.assertTrue(detail.isTruncated());
        byte[] binary = {0, (byte) 255, 1, 2};
        client.getBucket(prefix + ":binary", ByteArrayCodec.INSTANCE).set(binary);
        detail = management.detail(configure, request("binary"));
        Assert.assertEquals("base64", detail.getEncoding());
        Assert.assertArrayEquals(binary, Base64.getDecoder().decode(detail.getContent()));
    }

    @Test
    public void listAndSortedSetValuesPageWithoutReadingTheWholeCollection()
    {
        client.getList(prefix + ":list", StringCodec.INSTANCE).addAll(List.of("a", "b", "c", "d", "e"));
        ManagementRequest request = request("list");
        request.setLimit(2);
        ManagementDetail first = management.detail(configure, request);
        Assert.assertEquals(2, first.getRows().size());
        Assert.assertEquals("2", first.getNextCursor());
        request.setCursor(first.getNextCursor());
        ManagementDetail next = management.detail(configure, request);
        Assert.assertEquals("2", next.getRows().get(0).get("position"));
        Assert.assertEquals("c", next.getRows().get(0).get("value"));
        client.getScoredSortedSet(prefix + ":zset", StringCodec.INSTANCE).add(1.5, "member");
        Assert.assertEquals("1.5", management.detail(configure, request("zset")).getRows().get(0).get("position"));
    }

    @Test
    public void hashAndSetValuesUseTheirNativeScanTypes()
    {
        client.getMap(prefix + ":hash", StringCodec.INSTANCE).putAll(Map.of("name", "用户", "long", "x".repeat(3000)));
        ManagementDetail hash = management.detail(configure, request("hash"));
        Assert.assertTrue(hash.getRows().stream().anyMatch(row -> row.get("position").equals("name") && row.get("value").equals("用户")));
        Assert.assertTrue(hash.getRows().stream().anyMatch(row -> row.get("position").equals("long") && row.get("value").length() == 2048 && row.get("truncated").equals("true")));
        client.getSet(prefix + ":set", StringCodec.INSTANCE).add("hello");
        Assert.assertEquals("hello", management.detail(configure, request("set")).getRows().get(0).get("value"));
    }

    @Test
    public void streamIdsAndQuotedKeyNamesRemainDataRatherThanCommands()
    {
        String stream = prefix + ":stream";
        for (int i = 0; i < 3; i++) {
            client.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE,
                    "return redis.call('XADD',ARGV[1],'*','field',ARGV[2])", RScript.ReturnType.VALUE, List.of(), stream, "message " + i);
        }
        ManagementRequest request = request("stream");
        request.setLimit(2);
        ManagementDetail first = management.detail(configure, request);
        Assert.assertEquals(2, first.getRows().size());
        Assert.assertTrue(first.isHasMore());
        request.setCursor(first.getNextCursor());
        ManagementDetail last = management.detail(configure, request);
        Assert.assertEquals(1, last.getRows().size());
        Assert.assertFalse(last.isHasMore());
        client.getBucket(prefix + ":quote'雪/", StringCodec.INSTANCE).set("literal");
        Assert.assertEquals("literal", management.detail(configure, request("quote'雪/")).getContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void databaseScopeCannotBeChangedByARequest()
    {
        ManagementRequest request = request("key");
        request.setNamespace("1");
        management.detail(configure, request);
    }
}
