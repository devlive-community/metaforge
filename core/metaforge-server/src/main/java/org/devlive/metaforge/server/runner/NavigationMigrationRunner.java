package org.devlive.metaforge.server.runner;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.server.service.NavigationMigration;
import org.devlive.metaforge.service.initializer.InstallationState;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;

@Slf4j
@Component
@Order(1)
public class NavigationMigrationRunner
        implements CommandLineRunner
{
    private final DataSource dataSource;
    private final InstallationState installationState;

    public NavigationMigrationRunner(DataSource dataSource, InstallationState installationState)
    {
        this.dataSource = dataSource;
        this.installationState = installationState;
    }

    @Override
    public void run(String... args)
            throws Exception
    {
        if (!installationState.isInstalled()) {
            log.info("Navigation migration skipped: instance is not installed");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            NavigationMigration.apply(connection);
        }
    }
}
