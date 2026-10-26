package org.devlive.metaforge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "org.devlive.metaforge.service.repository",
        repositoryImplementationPostfix = "Impl",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager")
@EnableTransactionManagement
@EnableJpaAuditing
public class DataJpaConfigure
{
    @Value(value = "${metaforge.audit.sql.print}")
    private boolean sqlPrint;

    @Bean
    public JpaVendorAdapter jpaVendorAdapter()
    {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setShowSql(sqlPrint);
        jpaVendorAdapter.setGenerateDdl(false);
        return jpaVendorAdapter;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, JpaVendorAdapter jpaVendorAdapter)
    {
        LocalContainerEntityManagerFactoryBean managerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        managerFactoryBean.setDataSource(dataSource);
        managerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        managerFactoryBean.setPackagesToScan("org.devlive.metaforge.service.entity");
        return managerFactoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory managerFactory)
    {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(managerFactory);
        return transactionManager;
    }
}
