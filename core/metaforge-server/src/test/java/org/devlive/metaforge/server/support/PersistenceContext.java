package org.devlive.metaforge.server.support;

import org.devlive.metaforge.server.migration.DatabaseMigrator;
import org.devlive.metaforge.service.DataJdbcConfigure;
import org.devlive.metaforge.service.persistence.FilterQuery;
import org.devlive.metaforge.service.persistence.Persistence;
import org.devlive.metaforge.service.persistence.References;
import org.devlive.metaforge.service.repository.jdbc.LinkRepository;
import org.devlive.metaforge.service.repository.jdbc.MenuAccessRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import java.sql.Connection;

/**
 * The production persistence stack (Spring Data JDBC, filters, reference loading, link tables)
 * on a migrated test database, without the web application around it.
 */
public final class PersistenceContext
        implements AutoCloseable
{
    private final TestDatabase database;
    private final AnnotationConfigApplicationContext context;

    private PersistenceContext(TestDatabase database, AnnotationConfigApplicationContext context)
    {
        this.database = database;
        this.context = context;
    }

    public static PersistenceContext start(String spec)
            throws Exception
    {
        TestDatabase database = TestDatabase.start(spec);
        DriverManagerDataSource dataSource = new DriverManagerDataSource(database.url(), database.username(), database.password());
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMigrator.migrate(connection);
        }
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> dataSource);
        context.register(Beans.class);
        context.refresh();
        return new PersistenceContext(database, context);
    }

    public <T> T bean(Class<T> type)
    {
        return context.getBean(type);
    }

    public JdbcTemplate jdbc()
    {
        return new JdbcTemplate(context.getBean(DataSource.class));
    }

    public TestDatabase database()
    {
        return database;
    }

    @Override
    public void close()
    {
        context.close();
        database.close();
    }

    @Configuration
    @Import({DataJdbcConfigure.class, FilterQuery.class, References.class, Persistence.class, LinkRepository.class, MenuAccessRepository.class})
    static class Beans
    {
        @Bean
        NamedParameterJdbcOperations namedParameterJdbcOperations(DataSource dataSource)
        {
            return new NamedParameterJdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource)
        {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
