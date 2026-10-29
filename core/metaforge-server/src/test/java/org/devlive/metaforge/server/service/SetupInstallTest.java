package org.devlive.metaforge.server.service;

import org.devlive.metaforge.server.support.TestDatabase;
import org.devlive.metaforge.service.initializer.DataSetConfigure;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * The setup wizard installs into the database selected with {@code -Dmetaforge.it.database}.
 */
public class SetupInstallTest
{
    private static TestDatabase database;

    @BeforeAll
    public static void start()
    {
        database = TestDatabase.start(TestDatabase.selectedSpec());
    }

    @AfterAll
    public static void stop()
    {
        database.close();
    }

    @Test
    public void installsSchemaCatalogueAndAdministratorOnTheConfiguredDatabase()
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(database.url(), database.username(), database.password());
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Collections.singletonMap("spring.datasource.url", database.url())));
        InstallationState state = new InstallationState(dataSource);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        SetupService service = new SetupService(new JdbcTemplate(dataSource), dataSource, encoder, mock(DataSetConfigure.class), null,
                environment, new DefaultApplicationArguments(), state);

        assertFalse(state.isInstalled());
        SetupInstallBody body = SetupInstallBody.builder().username("admin").password("admin123").name("管理员")
                .database(SetupInstallBody.DatabaseConfigure.builder().type("current").build()).build();
        assertTrue(service.install(body).getStatus());
        assertTrue(state.isInstalled());

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertEquals("管理员", jdbc.queryForObject("SELECT name FROM metaforge_user WHERE username = 'admin'", String.class));
        assertTrue(encoder.matches("admin123", jdbc.queryForObject("SELECT password_hash FROM metaforge_user WHERE username = 'admin'", String.class)));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM metaforge_user_role ur JOIN metaforge_user u ON u.id = ur.user_id "
                + "JOIN metaforge_role r ON r.id = ur.role_id WHERE u.username = 'admin' AND r.code = 'ADMIN'", Long.class));
        // a second install is refused instead of duplicating the administrator
        assertFalse(service.install(body).getStatus());
    }

    @Test
    public void externalDatabasesBuildVendorUrls()
    {
        assertTrue(ExternalDatabase.of("mysql").orElseThrow().url("db", null, "metaforge").startsWith("jdbc:mysql://db:3306/metaforge?"));
        assertEquals("jdbc:postgresql://db:5432/metaforge", ExternalDatabase.of("PostgreSQL").orElseThrow().url("db", null, "metaforge"));
        assertTrue(ExternalDatabase.of("sqlserver").orElseThrow().url("db", 1444, "metaforge").startsWith("jdbc:sqlserver://db:1444;databaseName=metaforge;"));
        assertEquals("jdbc:oracle:thin:@//db:1521/FREEPDB1", ExternalDatabase.of("oracle").orElseThrow().url(" db ", null, "FREEPDB1"));
        assertFalse(ExternalDatabase.of("sqlite").isPresent());
    }
}
