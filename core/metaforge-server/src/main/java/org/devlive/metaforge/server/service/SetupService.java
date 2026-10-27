package org.devlive.metaforge.server.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.server.configure.SetupDatasourceGuardEnvironmentPostProcessor;
import org.devlive.metaforge.service.initializer.DataSetConfigure;
import org.devlive.metaforge.service.initializer.InitializerConfigure;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * First-boot installation: creates the schema, seeds baseline data and the
 * administrator account chosen in the setup wizard. All statements are
 * idempotent so a partially installed instance can safely re-run.
 *
 * The wizard can install into the datasource this instance already runs on,
 * or into a chosen target (embedded H2 file store or an external MySQL);
 * the chosen datasource is persisted to ./config/application.properties
 * which Spring Boot picks up on the next boot without extra launch flags.
 */
@Slf4j
@Service
public class SetupService
{
    private static final String CONFIG_OVERRIDE = "config/application.properties";
    private static final String MYSQL_URL_TEMPLATE =
            "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8"
                    + "&zeroDateTimeBehavior=convertToNull&useMultiQueries=true"
                    + "&useSSL=false&allowPublicKeyRetrieval=true";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final DataSetConfigure dataSetConfigure;
    private final InitializerConfigure initializer;
    private final Environment environment;
    private final ApplicationArguments applicationArguments;
    private final InstallationState installationState;

    public SetupService(JdbcTemplate jdbcTemplate, DataSource dataSource, PasswordEncoder passwordEncoder,
            DataSetConfigure dataSetConfigure, InitializerConfigure initializer, Environment environment,
            ApplicationArguments applicationArguments, InstallationState installationState)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
        this.dataSetConfigure = dataSetConfigure;
        this.initializer = initializer;
        this.environment = environment;
        this.applicationArguments = applicationArguments;
        this.installationState = installationState;
    }

    /**
     * Installed == the user table exists on the active datasource and holds
     * at least one account.
     */
    public boolean isInstalled()
    {
        return installationState.isInstalled();
    }

    public Map<String, Object> environment()
    {
        Map<String, Object> info = new HashMap<>();
        boolean configured = isDatasourceConfigured();
        String fallbackFrom = System.getProperty(SetupDatasourceGuardEnvironmentPostProcessor.FALLBACK_FROM_PROPERTY);
        info.put("datasourceConfigured", configured);
        info.put("degraded", fallbackFrom != null);
        info.put("degradedFrom", fallbackFrom);
        info.put("currentAvailable", isCurrentDatasourceAvailable());
        info.put("connected", Boolean.FALSE);
        // The temporary startup store is not a database selected by the user.
        if (!isCurrentDatasourceAvailable()) {
            info.put("installed", isInstalled());
            return info;
        }
        try (Connection connection = dataSource.getConnection()) {
            info.put("connected", Boolean.TRUE);
            info.put("database", connection.getCatalog());
            info.put("product", connection.getMetaData().getDatabaseProductName());
            info.put("version", connection.getMetaData().getDatabaseProductVersion());
            info.put("embedded", connection.getMetaData().getDatabaseProductName().contains("H2"));
        }
        catch (SQLException | DataAccessException e) {
            log.warn("Setup environment probe failed: {}", e.getMessage());
            info.put("connected", Boolean.FALSE);
            info.put("error", e.getMessage());
        }
        info.put("installed", isInstalled());
        return info;
    }

    private boolean isDatasourceConfigured()
    {
        return StringUtils.isNotBlank(environment.getProperty("spring.datasource.url"))
                || StringUtils.isNotBlank(environment.getProperty("spring.datasource.hikari.jdbc-url"));
    }

    private boolean isCurrentDatasourceAvailable()
    {
        return isDatasourceConfigured()
                && System.getProperty(SetupDatasourceGuardEnvironmentPostProcessor.FALLBACK_FROM_PROPERTY) == null;
    }

    public CommonResponse<Object> testConnection(SetupInstallBody.DatabaseConfigure configure)
    {
        long begin = System.currentTimeMillis();
        try {
            if (configure == null || StringUtils.isBlank(configure.getType())) {
                if (!isCurrentDatasourceAvailable()) {
                    return CommonResponse.failure("Choose a database target before testing the connection");
                }
                // no body: probe the active datasource
                jdbcTemplate.execute("SELECT 1");
                return successProbe(begin);
            }
            TargetDatabase target = resolveTarget(configure);
            try (Connection ignored = openTarget(target)) {
                return successProbe(begin);
            }
        }
        catch (Exception e) {
            log.warn("Setup connection test failed: {}", e.getMessage());
            return CommonResponse.failure(String.format("Connection failed: %s", e.getMessage()));
        }
    }

    private CommonResponse<Object> successProbe(long begin)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("connected", Boolean.TRUE);
        result.put("elapsed", System.currentTimeMillis() - begin);
        return CommonResponse.success(result);
    }

    public CommonResponse<Object> install(SetupInstallBody body)
    {
        if (isInstalled()) {
            return CommonResponse.failure("Instance is already installed");
        }
        if (body == null || StringUtils.isBlank(body.getUsername()) || StringUtils.isBlank(body.getPassword())) {
            return CommonResponse.failure("Username and password are required");
        }
        if (body.getUsername().length() < 3 || body.getUsername().length() > 20) {
            return CommonResponse.failure("Username must be 3-20 characters");
        }
        if (body.getPassword().length() < 6 || body.getPassword().length() > 20) {
            return CommonResponse.failure("Password must be 6-20 characters");
        }

        try {
            boolean restartRequired;
            if (body.getDatabase() == null || StringUtils.isBlank(body.getDatabase().getType())) {
                return CommonResponse.failure("Choose a database before installing");
            }
            if ("current".equalsIgnoreCase(body.getDatabase().getType())) {
                if (!isCurrentDatasourceAvailable()) {
                    return CommonResponse.failure("No configured datasource is available, choose a database target instead");
                }
                // install on the datasource this instance already runs on
                installOnActiveDatasource(body);
                restartRequired = false;
            }
            else {
                installOnTargetDatasource(body);
                restartRequired = true;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("username", body.getUsername());
            result.put("installed", Boolean.TRUE);
            result.put("restartRequired", restartRequired);
            if (restartRequired) {
                result.put("configPath", resolveOverrideFile().getAbsolutePath());
            }
            log.info("Setup finished, administrator [ {} ] created", body.getUsername());
            return CommonResponse.success(result);
        }
        catch (Exception e) {
            log.error("Setup failed", e);
            return CommonResponse.failure(String.format("Setup failed: %s", e.getMessage()));
        }
    }

    private void installOnActiveDatasource(SetupInstallBody body)
            throws Exception
    {
        boolean h2;
        try (Connection connection = dataSource.getConnection()) {
            h2 = connection.getMetaData().getDatabaseProductName().contains("H2");
        }
        try (Connection connection = dataSource.getConnection()) {
            executeScript(connection, h2 ? "schema/setup-schema-h2.sql" : "schema/setup-schema.sql");
            executeScript(connection, "schema/setup-data.sql");
            NavigationMigration.apply(connection);
            createAdministrator(connection, body);
        }
        reloadRuntimeConfigures();
    }

    private void installOnTargetDatasource(SetupInstallBody body)
            throws Exception
    {
        TargetDatabase target = resolveTarget(body.getDatabase());
        try (Connection connection = openTarget(target)) {
            executeScript(connection, target.embedded ? "schema/setup-schema-h2.sql" : "schema/setup-schema.sql");
            executeScript(connection, "schema/setup-data.sql");
            NavigationMigration.apply(connection);
            createAdministrator(connection, body);
        }
        persistDatasourceOverride(target);
        log.info("Datasource override persisted to {}, restart the server to activate it", CONFIG_OVERRIDE);
    }

    private void reloadRuntimeConfigures()
    {
        // re-run the seeds that were skipped while tables were missing
        try {
            dataSetConfigure.reload();
        }
        catch (Exception e) {
            log.warn("Reload dataset configure after setup failed: {}", e.getMessage());
        }
    }

    private TargetDatabase resolveTarget(SetupInstallBody.DatabaseConfigure configure)
    {
        if ("embedded".equalsIgnoreCase(configure.getType())) {
            String home = initializer.getDataHome();
            File dir = new File(home);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Cannot create data directory: " + home);
            }
            String url = String.format("jdbc:h2:file:%s;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE",
                    new File(dir, "metaforge").getAbsolutePath().replace('\\', '/'));
            return new TargetDatabase(url, "sa", "", true);
        }
        if ("mysql".equalsIgnoreCase(configure.getType())) {
            if (StringUtils.isAnyBlank(configure.getHost(), configure.getDatabase(), configure.getUsername())) {
                throw new IllegalArgumentException("Host, database and username are required for MySQL");
            }
            int port = configure.getPort() == null ? 3306 : configure.getPort();
            String url = String.format(MYSQL_URL_TEMPLATE, configure.getHost(), port, configure.getDatabase());
            return new TargetDatabase(url, configure.getUsername(), configure.getPassword() == null ? "" : configure.getPassword(), false);
        }
        throw new IllegalArgumentException("Unknown database type: " + configure.getType());
    }

    private Connection openTarget(TargetDatabase target)
            throws SQLException
    {
        return DriverManager.getConnection(target.url, target.username, target.password);
    }

    /**
     * Spring Boot reads ./config/application.properties by default, so the
     * wizard can persist the chosen datasource without extra launch flags.
     */
    private void persistDatasourceOverride(TargetDatabase target)
            throws IOException
    {
        File override = resolveOverrideFile();
        Map<String, String> lines = new LinkedHashMap<>();
        if (override.exists()) {
            for (String line : Files.readAllLines(override.toPath(), StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    lines.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
                }
            }
        }
        lines.put("spring.datasource.url", target.url);
        lines.put("spring.datasource.username", target.username);
        lines.put("spring.datasource.password", target.password);
        lines.put("spring.jpa.hibernate.ddl-auto", "none");
        StringBuilder content = new StringBuilder("# Written by the MetaForge setup wizard\n");
        for (Map.Entry<String, String> entry : lines.entrySet()) {
            content.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        if (override.getParentFile() != null && !override.getParentFile().exists()) {
            override.getParentFile().mkdirs();
        }
        Files.write(override.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
    }

    private File resolveOverrideFile()
    {
        // --spring.config.location is a framework arg stripped from the
        // environment, so read it from the raw application arguments first
        String location = null;
        List<String> values = applicationArguments.getOptionValues("spring.config.location");
        if (values != null && !values.isEmpty()) {
            location = String.join(",", values);
        }
        if (location == null || location.trim().isEmpty()) {
            location = environment.getProperty("spring.config.additional-location");
        }
        if (location != null && !location.trim().isEmpty()) {
            for (String candidate : location.split(",")) {
                String cleaned = candidate.trim();
                if (cleaned.isEmpty()) {
                    continue;
                }
                cleaned = cleaned.replace("file:", "");
                if (cleaned.endsWith(".properties")) {
                    return new File(cleaned);
                }
                File dir = new File(cleaned);
                if (dir.isDirectory()) {
                    return new File(dir, "application.properties");
                }
            }
        }
        return new File(CONFIG_OVERRIDE);
    }

    private void executeScript(Connection connection, String path)
            throws SQLException
    {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource(path));
    }

    private void createAdministrator(Connection connection, SetupInstallBody body)
            throws SQLException
    {
        String username = body.getUsername();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format(
                    "INSERT INTO metaforge_user (username, password, name, is_system, active, code) VALUES ('%s', '%s', '%s', 1, 1, '%s')",
                    escape(username),
                    escape(passwordEncoder.encode(body.getPassword())),
                    escape(StringUtils.defaultIfBlank(body.getName(), username)),
                    UUID.randomUUID().toString().replace("-", "")));
            // role id 1 is the seeded system ADMIN role
            statement.executeUpdate(String.format(
                    "INSERT INTO metaforge_user_role_relation (user_id, role_id) SELECT id, 1 FROM metaforge_user WHERE username = '%s'",
                    escape(username)));
        }
    }

    private String escape(String value)
    {
        return value == null ? "" : value.replace("'", "''");
    }

    private static class TargetDatabase
    {
        private final String url;
        private final String username;
        private final String password;
        private final boolean embedded;

        private TargetDatabase(String url, String username, String password, boolean embedded)
        {
            this.url = url;
            this.username = username;
            this.password = password;
            this.embedded = embedded;
        }
    }
}
