package org.devlive.metaforge.server.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * External metadata databases the setup wizard can install into.
 */
public enum ExternalDatabase
{
    MYSQL("mysql", "MySQL", 3306, "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8"
            + "&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true"),
    POSTGRESQL("postgresql", "PostgreSQL", 5432, "jdbc:postgresql://%s:%d/%s"),
    SQLSERVER("sqlserver", "SQL Server", 1433, "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true"),
    // the database field holds the Oracle service name
    ORACLE("oracle", "Oracle", 1521, "jdbc:oracle:thin:@//%s:%d/%s");

    private final String type;
    private final String label;
    private final int defaultPort;
    private final String urlTemplate;

    ExternalDatabase(String type, String label, int defaultPort, String urlTemplate)
    {
        this.type = type;
        this.label = label;
        this.defaultPort = defaultPort;
        this.urlTemplate = urlTemplate;
    }

    public static Optional<ExternalDatabase> of(String type)
    {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(value -> value.type.equals(normalized)).findFirst();
    }

    public String url(String host, Integer port, String database)
    {
        return String.format(urlTemplate, host.trim(), port == null ? defaultPort : port, database.trim());
    }

    public String getLabel()
    {
        return label;
    }
}
