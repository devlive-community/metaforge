package org.devlive.metaforge.server.configure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * First-boot guard: when the configured datasource is unreachable (wrong
 * host, missing database, bad credentials) the server would die before the
 * setup wizard could ever load. Probe the connection before the context
 * refreshes; on failure swap in an in-memory H2 fallback so the wizard can
 * boot and the user can fix the database from the browser.
 *
 * The original url is exposed through the system property
 * `metaforge.setup.fallback.from` and surfaced by /api/v1/setup/environment.
 */
public class SetupDatasourceGuardEnvironmentPostProcessor
        implements EnvironmentPostProcessor
{
    public static final String FALLBACK_FROM_PROPERTY = "metaforge.setup.fallback.from";

    private static final String FALLBACK_URL = "jdbc:h2:mem:metaforge-setup;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final int PROBE_TIMEOUT_SECONDS = 6;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        appendClasspathDefaults(environment);

        String url = environment.getProperty("spring.datasource.url");
        if (url == null || url.trim().isEmpty() || url.contains(":h2:")) {
            // nothing configured (boot falls back to embedded h2) or already h2
            return;
        }
        if (System.getProperty(FALLBACK_FROM_PROPERTY) != null) {
            // already degraded in an earlier attempt within this jvm
            return;
        }
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        if (!probe(url, username, password)) {
            System.err.printf("[metaforge-setup] Datasource unavailable (%s), booting on the embedded fallback store; open /setup to configure the database%n", url);
            System.setProperty(FALLBACK_FROM_PROPERTY, url);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("spring.datasource.url", FALLBACK_URL);
            fallback.put("spring.datasource.username", "sa");
            fallback.put("spring.datasource.password", "");
            fallback.put("spring.datasource.driver-class-name", "org.h2.Driver");
            environment.getPropertySources().addFirst(new MapPropertySource("metaforgeSetupFallback", fallback));
        }
    }

    /**
     * spring.config.location replaces the default locations entirely, which
     * would drop the classpath defaults for every metaforge.* key; append
     * them at the lowest priority so partial external configs still boot.
     */
    private void appendClasspathDefaults(ConfigurableEnvironment environment)
    {
        if (environment.getPropertySources().contains("metaforgeClasspathDefaults")) {
            return;
        }
        ClassPathResource resource = new ClassPathResource("application.properties");
        if (!resource.exists()) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = resource.getInputStream()) {
            properties.load(input);
        }
        catch (IOException e) {
            return;
        }
        environment.getPropertySources().addLast(new PropertiesPropertySource("metaforgeClasspathDefaults", properties));
    }

    private boolean probe(String url, String username, String password)
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> future = executor.submit(() -> {
                try (Connection ignored = DriverManager.getConnection(url, username == null ? "" : username, password == null ? "" : password)) {
                    return true;
                }
                catch (Exception e) {
                    System.err.printf("[metaforge-setup] Datasource probe failed: %s%n", e.getMessage());
                    return false;
                }
            });
            return future.get(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            return false;
        }
        finally {
            executor.shutdownNow();
        }
    }
}
