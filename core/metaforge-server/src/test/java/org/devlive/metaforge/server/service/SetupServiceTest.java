package org.devlive.metaforge.server.service;

import org.devlive.metaforge.server.configure.SetupDatasourceGuardEnvironmentPostProcessor;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class SetupServiceTest
{
    private final DataSource dataSource = mock(DataSource.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final StandardEnvironment environment = new StandardEnvironment();
    private final SetupService service = new SetupService(jdbcTemplate, dataSource, null, null, null,
            environment, new DefaultApplicationArguments(), mock(InstallationState.class));

    @AfterEach
    public void clearFallback()
    {
        System.clearProperty(SetupDatasourceGuardEnvironmentPostProcessor.FALLBACK_FROM_PROPERTY);
    }

    @Test
    public void testUnconfiguredDatabaseDoesNotExposeStartupH2()
    {
        Map<String, Object> result = service.environment();
        assertEquals(false, result.get("datasourceConfigured"));
        assertEquals(false, result.get("currentAvailable"));
        assertEquals(false, result.get("connected"));
        assertFalse(result.containsKey("product"));
        verifyNoInteractions(dataSource, jdbcTemplate);
    }

    @Test
    public void testDegradedDatabaseIsNotAvailableForInstallation()
    {
        String url = "jdbc:mysql://unavailable/metaforge";
        environment.getPropertySources().addFirst(new MapPropertySource("test", Collections.singletonMap("spring.datasource.url", url)));
        System.setProperty(SetupDatasourceGuardEnvironmentPostProcessor.FALLBACK_FROM_PROPERTY, url);

        Map<String, Object> result = service.environment();
        assertEquals(true, result.get("degraded"));
        assertEquals(false, result.get("currentAvailable"));
        assertFalse(result.containsKey("product"));
        assertFalse(service.install(body("current")).getStatus());
        verifyNoInteractions(dataSource, jdbcTemplate);
    }

    @Test
    public void testDatabaseChoiceIsRequiredAndTemporaryStoreCannotBeInstalled()
    {
        assertFalse(service.install(body(null)).getStatus());
        assertFalse(service.install(body("current")).getStatus());
        assertFalse(service.testConnection(null).getStatus());
        verifyNoInteractions(dataSource, jdbcTemplate);
    }

    @Test
    public void testBlankUrlDoesNotCountAsConfigured()
    {
        environment.getPropertySources().addFirst(new MapPropertySource("test", Collections.singletonMap("spring.datasource.url", " ")));
        assertEquals(false, service.environment().get("currentAvailable"));
        assertTrue(service.install(body(null)).getMessage().toString().contains("Choose a database"));
    }

    @Test
    public void testExplicitlyConfiguredH2RemainsAvailable()
    {
        String url = "jdbc:h2:mem:" + UUID.randomUUID();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Collections.singletonMap("spring.datasource.url", url)));
        DriverManagerDataSource configured = new DriverManagerDataSource(url, "sa", "");
        SetupService configuredService = new SetupService(new JdbcTemplate(configured), configured, null, null, null,
                environment, new DefaultApplicationArguments(), new InstallationState(configured));

        Map<String, Object> result = configuredService.environment();
        assertEquals(true, result.get("currentAvailable"));
        assertEquals(true, result.get("connected"));
        assertEquals("H2", result.get("product"));
        assertTrue(configuredService.testConnection(null).getStatus());
    }

    private SetupInstallBody body(String type)
    {
        return SetupInstallBody.builder()
                .username("admin")
                .password("test-password")
                .database(type == null ? null : SetupInstallBody.DatabaseConfigure.builder().type(type).build())
                .build();
    }
}
