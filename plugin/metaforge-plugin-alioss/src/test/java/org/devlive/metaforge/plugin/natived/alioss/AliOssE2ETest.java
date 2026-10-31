package org.devlive.metaforge.plugin.natived.alioss;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.RuntimeConfigure;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.util.List;

import static org.devlive.metaforge.test.e2e.PluginE2E.column;
import static org.devlive.metaforge.test.e2e.PluginE2E.configure;
import static org.devlive.metaforge.test.e2e.PluginE2E.ok;

/**
 * Runs the Aliyun OSS plugin through MetaForge's OSS JDBC driver against a real bucket: OSS is a
 * service with no server to run locally. Set OSS_E2E_ENDPOINT (such as oss-cn-hangzhou.aliyuncs.com),
 * OSS_E2E_ACCESS_KEY_ID, OSS_E2E_ACCESS_KEY_SECRET and OSS_E2E_BUCKET to run it; it works under a
 * prefix of its own and removes it.
 */
public class AliOssE2ETest
{
    private static String env(String name)
    {
        String value = System.getenv(name);
        Assume.assumeTrue(name + " is not set, skipping the Aliyun OSS e2e test", value != null && !value.isEmpty());
        return value;
    }

    @Test
    public void prefixesAndObjectsAreManagedWithSql()
    {
        String endpoint = env("OSS_E2E_ENDPOINT");
        String bucket = env("OSS_E2E_BUCKET");
        RuntimeConfigure configure = configure(endpoint, 0, env("OSS_E2E_ACCESS_KEY_ID"), env("OSS_E2E_ACCESS_KEY_SECRET"), bucket);
        configure.setPort(null);
        PluginService service = new AliOssService();
        String table = "metaforge_e2e_" + System.currentTimeMillis();
        String name = "`" + bucket + "`." + table;

        ok("validator", service.execute(configure, service.validator()));
        Assert.assertTrue(column(ok("databases", service.getDatabases(configure)), "object_name").contains(bucket));
        try {
            ok("create table", service.execute(configure, "CREATE TABLE " + name + " (`key` VARCHAR)"));
            Assert.assertTrue(column(ok("tables", service.getTables(configure, bucket)), "object_name").contains(table));
            ok("insert", service.execute(configure, "INSERT INTO " + name + " (`key`, content) VALUES ('a.txt', 'hello'), ('b.txt', 'world')"));
            Assert.assertEquals(List.of("a.txt", "b.txt"), column(ok("select", service.execute(configure, "SELECT `key`, size FROM " + name + " ORDER BY `key`")), "key"));
            ok("rename", service.execute(configure, "UPDATE " + name + " SET `key` = 'c.txt' WHERE `key` = 'a.txt'"));
            ok("delete", service.execute(configure, "DELETE FROM " + name + " WHERE `key` = 'b.txt'"));
            Assert.assertEquals(List.of("c.txt"), column(ok("left", service.execute(configure, "SELECT `key` FROM " + name)), "key"));
        }
        finally {
            ok("drop table", service.execute(configure, "DROP TABLE " + name));
        }
        Assert.assertFalse(column(ok("tables", service.getTables(configure, bucket)), "object_name").contains(table));
    }

    @Test
    public void theEndpointGivesTheUrl()
    {
        RuntimeConfigure configure = configure("https://oss-cn-hangzhou.aliyuncs.com/", 0, "id", "secret", "lake");
        configure.setPort(null);
        Assert.assertEquals("jdbc:alioss://oss-cn-hangzhou.aliyuncs.com/lake?ssl=true", new AliOssService().url(configure));
        configure.setHost("http://oss-internal.example");
        configure.setDatabase(java.util.Optional.empty());
        Assert.assertEquals("jdbc:alioss://oss-internal.example?ssl=false", new AliOssService().url(configure));
    }
}
