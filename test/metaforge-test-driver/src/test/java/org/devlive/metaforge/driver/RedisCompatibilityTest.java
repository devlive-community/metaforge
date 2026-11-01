package org.devlive.metaforge.driver;

import com.dbschema.MongoJdbcDriver;
import io.edurt.datacap.driver.cluster.RedisClusterDriver;
import org.devlive.metaforge.MongoDriver;
import org.devlive.metaforge.RedisDriver;
import org.junit.Test;

import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** What the DataCap 2025.1.0 drivers accepted keeps working, without a Redis server. */
public class RedisCompatibilityTest
{
    @Test
    public void releasedClassNamesStillLoadTheDrivers()
            throws Exception
    {
        for (String name : new String[] {"io.edurt.datacap.driver.redis.RedisDriver", "io.edurt.datacap.driver.cluster.RedisClusterDriver"}) {
            Class.forName(name);
            assertTrue(name, RedisDriver.class.isAssignableFrom(Class.forName(name)));
        }
        assertTrue(DriverManager.getDriver("jdbc:redis://localhost:6379") instanceof RedisDriver);
        assertTrue(new RedisClusterDriver().acceptsURL("jdbc:redis-cluster://a:7000,b:7001"));

        for (String name : new String[] {"io.edurt.datacap.driver.MongoJdbcDriver", "com.dbschema.MongoJdbcDriver"}) {
            assertTrue(name, MongoDriver.class.isAssignableFrom(Class.forName(name)));
        }
        assertTrue(new MongoJdbcDriver().acceptsURL("jdbc:mongodb://localhost:27017/app"));
        assertTrue(DriverManager.getDriver("jdbc:mongo://localhost:27017") instanceof MongoDriver);
    }

    @Test
    public void addressesAndDatabasesComeFromTheUrl()
    {
        assertEquals(Collections.singletonList("redis://cache:6380"), RedisConnection.addresses("jdbc:redis://cache:6380/2?timeout=5"));
        assertEquals(Collections.singletonList("redis://cache:6379"), RedisConnection.addresses("jdbc:redis://cache"));
        assertEquals(Arrays.asList("redis://a:7000", "redis://b:7001"), RedisConnection.addresses("jdbc:redis-cluster://a:7000, b:7001"));
        assertTrue(RedisConnection.isCluster("jdbc:redis-cluster://a:7000"));
        assertFalse(RedisConnection.isCluster("jdbc:redis://a:7000"));

        assertEquals(2, RedisConnection.database("jdbc:redis://cache:6380/2", new Properties()));
        Properties info = new Properties();
        info.setProperty("database", "5");
        assertEquals(5, RedisConnection.database("jdbc:redis://cache:6380/2", info));
        assertEquals(0, RedisConnection.database("jdbc:redis://cache:6380", new Properties()));
    }

    @Test
    public void plainCommandsAreToldApartFromTheSqlSubset()
    {
        assertTrue(RedisCommands.isCommand("GET user:1"));
        assertTrue(RedisCommands.isCommand("hgetall \"my hash\""));
        assertTrue(RedisCommands.isCommand("SELECT 2"));
        assertFalse(RedisCommands.isCommand("SELECT * FROM user"));
        assertFalse(RedisCommands.isCommand("show tables"));
        assertFalse(RedisCommands.isCommand("   "));

        assertEquals(Arrays.asList("SET", "greeting", "hello world", "it's"), RedisCommands.tokenize("SET greeting \"hello world\" 'it\\'s';"));
        assertEquals(Arrays.asList("GET", ""), RedisCommands.tokenize("GET \"\""));
    }

    @Test
    public void repliesBecomeOneResultsColumn()
    {
        List<Map<String, Object>> rows = RedisCommands.rows(Arrays.asList("a", Arrays.asList("b", "c")));
        assertEquals(3, rows.size());
        assertEquals("c", rows.get(2).get(RedisCommands.COLUMN));
        assertEquals(1, RedisCommands.rows(null).size());
        assertEquals(0, RedisCommands.rows(Collections.emptyList()).size());
        assertEquals("OK", RedisCommands.rows("OK").get(0).get(RedisCommands.COLUMN));
    }
}
