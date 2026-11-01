package io.edurt.datacap.driver.cluster;

import org.devlive.metaforge.RedisDriver;

/**
 * The class name the DataCap 2025.1.0 cluster driver was loaded by; {@link RedisDriver} takes
 * jdbc:redis-cluster://host:port,host:port itself.
 *
 * @deprecated use {@link RedisDriver}
 */
@Deprecated
public class RedisClusterDriver
        extends RedisDriver
{
}
