package org.devlive.metaforge.server.support;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.oracle.OracleContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

/**
 * A metadata database for the multi-database tests, selected by a spec such as {@code h2},
 * {@code mysql:8.0}, {@code postgres:16}, {@code sqlserver:2022} or {@code oracle:23}
 * (system property {@code metaforge.it.database}, default {@code h2}).
 * Every engine except H2 runs in a Testcontainers container.
 */
public final class TestDatabase
        implements AutoCloseable
{
    public static final String PROPERTY = "metaforge.it.database";

    private final String engine;
    private final JdbcDatabaseContainer<?> container;
    private final String h2Url;

    private TestDatabase(String engine, JdbcDatabaseContainer<?> container)
    {
        this.engine = engine;
        this.container = container;
        // Same flags as the embedded store the setup wizard creates.
        this.h2Url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    }

    public static String selectedSpec()
    {
        return System.getProperty(PROPERTY, "h2");
    }

    public static TestDatabase h2()
    {
        return start("h2");
    }

    public static TestDatabase start(String spec)
    {
        String[] parts = spec.trim().toLowerCase(Locale.ROOT).split(":", 2);
        String engine = parts[0];
        String version = parts.length > 1 ? parts[1] : "";
        JdbcDatabaseContainer<?> container;
        switch (engine) {
            case "h2":
                container = null;
                break;
            case "mysql":
                container = new MySQLContainer<>("mysql:" + version)
                        .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");
                break;
            case "postgres":
                container = new PostgreSQLContainer<>("postgres:" + version);
                break;
            case "sqlserver":
                container = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:" + version + "-latest").acceptLicense();
                break;
            case "oracle":
                container = new OracleContainer("gvenzl/oracle-free:" + version + "-slim-faststart");
                break;
            default:
                throw new IllegalArgumentException("Unknown database '" + spec + "'; use h2, mysql:<v>, postgres:<v>, sqlserver:<v> or oracle:<v>");
        }
        if (container != null) {
            // docker-java's default API version is rejected by recent Docker engines
            if (System.getProperty("api.version") == null && System.getenv("DOCKER_API_VERSION") == null) {
                System.setProperty("api.version", "1.41");
            }
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                throw new IllegalStateException("Docker is required for database " + spec);
            }
            container.start();
        }
        return new TestDatabase(engine, container);
    }

    public String engine()
    {
        return engine;
    }

    public boolean isH2()
    {
        return container == null;
    }

    public String url()
    {
        return container == null ? h2Url : container.getJdbcUrl();
    }

    public String username()
    {
        return container == null ? "sa" : container.getUsername();
    }

    public String password()
    {
        return container == null ? "" : container.getPassword();
    }

    public Connection connect()
            throws SQLException
    {
        return DriverManager.getConnection(url(), username(), password());
    }

    @Override
    public void close()
    {
        if (container != null) {
            container.stop();
        }
    }

    @Override
    public String toString()
    {
        return engine;
    }
}
