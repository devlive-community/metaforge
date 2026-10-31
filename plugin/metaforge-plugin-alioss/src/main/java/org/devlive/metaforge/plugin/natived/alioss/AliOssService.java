package org.devlive.metaforge.plugin.natived.alioss;

import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.model.Response;
import org.devlive.metaforge.spi.model.RuntimeConfigure;

import java.util.stream.Collectors;

/**
 * Aliyun OSS through MetaForge's OSS JDBC driver: a bucket is a database, a top-level prefix a
 * table and the objects under it rows. Buckets, prefixes and their columns come from the
 * driver's JDBC metadata; the AccessKey ID and secret are the username and password.
 */
public class AliOssService
        implements PluginService
{
    @Override
    public String driver()
    {
        return "org.devlive.metaforge.AliOssDriver";
    }

    @Override
    public String connectType()
    {
        return "alioss";
    }

    /** The host is the endpoint, with or without its scheme; http:// turns TLS off. */
    @Override
    public String url(RuntimeConfigure configure)
    {
        if (configure.getUrl().isPresent()) {
            return configure.getUrl().get();
        }
        String host = configure.getHost().trim();
        boolean ssl = !host.startsWith("http://");
        host = host.replaceFirst("^https?://", "").replaceAll("/+$", "");
        StringBuilder url = new StringBuilder("jdbc:alioss://").append(host);
        configure.getDatabase().filter(bucket -> !bucket.isEmpty()).ifPresent(bucket -> url.append("/").append(bucket));
        url.append("?ssl=").append(ssl);
        configure.getEnv().filter(env -> !env.isEmpty()).ifPresent(env -> url.append("&").append(env.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"))));
        return url.toString();
    }

    @Override
    public Response getDatabases(RuntimeConfigure configure)
    {
        return metadataDatabases(configure);
    }

    @Override
    public Response getTables(RuntimeConfigure configure, String database)
    {
        return metadataTables(configure, database);
    }

    @Override
    public Response getColumns(RuntimeConfigure configure, String database, String table)
    {
        return metadataColumns(configure, database, table);
    }
}
