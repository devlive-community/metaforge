package org.devlive.metaforge.driver.sql;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** jdbc:&lt;name&gt;://host[:port][,host[:port]...][/database][?key=value&amp;...], with properties overriding the query. */
public final class ConnectionUrl
{
    private final String hosts;
    private final String database;
    private final Map<String, String> parameters;

    private ConnectionUrl(String hosts, String database, Map<String, String> parameters)
    {
        this.hosts = hosts;
        this.database = database;
        this.parameters = parameters;
    }

    public static ConnectionUrl parse(String prefix, String url, Properties info)
            throws SQLException
    {
        if (url == null || !url.startsWith(prefix)) {
            throw new SQLException("Not a " + prefix + " URL: " + url);
        }
        String rest = url.substring(prefix.length());
        Map<String, String> parameters = new LinkedHashMap<>();
        int question = rest.indexOf('?');
        if (question >= 0) {
            for (String pair : rest.substring(question + 1).split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int equals = pair.indexOf('=');
                String key = URLDecoder.decode(equals < 0 ? pair : pair.substring(0, equals), StandardCharsets.UTF_8);
                String value = equals < 0 ? "" : URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
                parameters.put(key, value);
            }
            rest = rest.substring(0, question);
        }
        if (info != null) {
            info.stringPropertyNames().forEach(name -> parameters.put(name, info.getProperty(name)));
        }
        int slash = rest.indexOf('/');
        String hosts = slash < 0 ? rest : rest.substring(0, slash);
        String database = slash < 0 ? "" : URLDecoder.decode(rest.substring(slash + 1), StandardCharsets.UTF_8);
        if (hosts.isEmpty()) {
            throw new SQLException("No host in " + url);
        }
        return new ConnectionUrl(hosts, database, parameters);
    }

    /** host:port pairs as written, comma separated. */
    public String getHosts()
    {
        return hosts;
    }

    public String getDatabase()
    {
        return database;
    }

    public String get(String key, String fallback)
    {
        String value = parameters.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    public Map<String, String> getParameters()
    {
        return new LinkedHashMap<>(parameters);
    }
}
