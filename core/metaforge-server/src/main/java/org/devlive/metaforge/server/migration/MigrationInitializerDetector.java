package org.devlive.metaforge.server.migration;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;

import java.util.Collections;
import java.util.Set;

/**
 * Makes JDBC templates, repositories and other database-dependent beans wait for {@link StartupMigration}.
 */
public class MigrationInitializerDetector
        extends AbstractBeansOfTypeDatabaseInitializerDetector
{
    @Override
    protected Set<Class<?>> getDatabaseInitializerBeanTypes()
    {
        return Collections.singleton(StartupMigration.class);
    }
}
