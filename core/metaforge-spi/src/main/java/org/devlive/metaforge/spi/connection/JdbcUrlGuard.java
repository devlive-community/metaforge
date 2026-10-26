package org.devlive.metaforge.spi.connection;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * JDBC URL 拼接防护。host、database 与 URL 参数均来自用户输入并被直接拼接进
 * JDBC URL，不校验时可以通过注入参数分隔符构造任意 JDBC 参数，例如 H2 的
 * INIT 参数远程命令执行、MySQL 的 allowLoadLocalInfile 任意文件读取。
 */
public final class JdbcUrlGuard
{
    private static final Pattern SAFE_HOST = Pattern.compile("[A-Za-z0-9.\\-_:\\[\\]]+");
    private static final Pattern SAFE_DATABASE = Pattern.compile("[^\\s?&#;=%'\"\\\\`/]+");
    private static final Pattern SAFE_PARAMETER = Pattern.compile("[^\\s?&#;=%'\"\\\\`]+");
    private static final Set<String> FORBIDDEN_PARAMETERS = Set.of(
            "init", "runscript", "exec",
            "allowloadlocalinfile", "allowurlinlocalinfile", "autodeserialize",
            "queryinterceptors", "statementinterceptors");

    private JdbcUrlGuard()
    {}

    public static String validateHost(String host)
    {
        if (host == null || !SAFE_HOST.matcher(host).matches()) {
            throw new IllegalArgumentException("Invalid host in JDBC url: " + host);
        }
        return host;
    }

    public static void validateDatabase(String database)
    {
        if (database == null || !SAFE_DATABASE.matcher(database).matches()) {
            throw new IllegalArgumentException("Invalid database in JDBC url: " + database);
        }
    }

    public static void validateParameter(String key, String value)
    {
        if (key == null || !SAFE_PARAMETER.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid JDBC url parameter: " + key);
        }
        if (value == null || !SAFE_PARAMETER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid value for JDBC url parameter " + key + ": " + value);
        }
        String normalized = key.toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "");
        if (FORBIDDEN_PARAMETERS.contains(normalized)) {
            throw new IllegalArgumentException("JDBC url parameter is not allowed: " + key);
        }
    }
}
