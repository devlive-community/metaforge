package org.devlive.metaforge.service;

import org.devlive.metaforge.service.entity.BaseEntity;
import org.devlive.metaforge.service.persistence.JdbcConverters;
import org.devlive.metaforge.service.persistence.References;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Plain JDBC persistence: aggregates map to single tables, no lazy loading, no implicit associations.
 */
@Configuration
@EnableJdbcRepositories(basePackages = "org.devlive.metaforge.service.repository")
@EnableJdbcAuditing
@EnableTransactionManagement
public class DataJdbcConfigure
        extends AbstractJdbcConfiguration
{
    @Override
    @Bean
    public JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy, JdbcCustomConversions customConversions)
    {
        JdbcMappingContext context = super.jdbcMappingContext(namingStrategy, customConversions);
        // Unquoted names fold to each database's own case (upper on Oracle, lower on PostgreSQL),
        // matching the unquoted names the changelog creates.
        context.setForceQuote(false);
        return context;
    }

    @Override
    protected List<?> userConverters()
    {
        return JdbcConverters.all();
    }

    @Bean
    public BeforeConvertCallback<Object> referenceIdCallback()
    {
        return aggregate -> {
            References.syncIds(aggregate);
            if (aggregate instanceof BaseEntity) {
                BaseEntity entity = (BaseEntity) aggregate;
                if (entity.getCode() == null) {
                    entity.setCode(UUID.randomUUID().toString().replace("-", ""));
                }
                entity.setUpdateTime(new Date());
            }
            return aggregate;
        };
    }
}
