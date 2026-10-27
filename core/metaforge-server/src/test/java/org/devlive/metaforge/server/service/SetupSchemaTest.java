package org.devlive.metaforge.server.service;

import org.devlive.metaforge.service.entity.ConfigureEntity;
import org.devlive.metaforge.service.entity.DatasetHistoryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManager;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SetupSchemaTest
{
    @Test
    public void testSetupSchemaSupportsCurrentEntitiesAndRepeatedInitialization()
            throws Exception
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        try {
            try (Connection connection = dataSource.getConnection()) {
                for (int attempt = 0; attempt < 2; attempt++) {
                    ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-schema-h2.sql"));
                    ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema/setup-data.sql"));
                    NavigationMigration.apply(connection);
                }
            }
            factory.setDataSource(dataSource);
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setPackagesToScan("org.devlive.metaforge.service.entity");
            factory.afterPropertiesSet();
            EntityManager manager = factory.getObject().createEntityManager();
            try {
                // Real entity projections reproduce the startup queries that
                // previously failed after installing an outdated schema.
                assertTrue(manager.createQuery("SELECT h FROM DatasetHistoryEntity h", DatasetHistoryEntity.class).getResultList().isEmpty());
                assertTrue(manager.createQuery("SELECT c FROM ConfigureEntity c", ConfigureEntity.class).getResultList().isEmpty());
            }
            finally {
                manager.close();
            }
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM metaforge_menu WHERE code = 'RUNTIME_CONFIGURE'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM metaforge_role_menu_relation r "
                    + "JOIN metaforge_menu m ON r.menu_id = CAST(m.id AS CHAR) WHERE r.role_id = '1' AND m.code = 'RUNTIME_CONFIGURE'", Integer.class));
        }
        finally {
            factory.destroy();
            new JdbcTemplate(dataSource).execute("SHUTDOWN");
        }
    }
}
