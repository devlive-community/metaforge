package org.devlive.metaforge.server.service;

import org.devlive.metaforge.service.initializer.InstallationState;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstallationStateTest
{
    @Test
    public void testFreshAndInstalledDatabaseWithBothH2IdentifierModes()
    {
        verifyInstallationState("");
        verifyInstallationState(";DATABASE_TO_LOWER=TRUE");
    }

    private void verifyInstallationState(String options)
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1" + options, "sa", "");
        InstallationState state = new InstallationState(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            assertFalse(state.isInstalled());
            jdbc.execute("CREATE TABLE metaforge_user (id BIGINT PRIMARY KEY)");
            assertFalse(state.isInstalled());
            jdbc.update("INSERT INTO metaforge_user (id) VALUES (1)");
            assertTrue(state.isInstalled());
        }
        finally {
            jdbc.execute("SHUTDOWN");
        }
    }
}
